package io.openems.edge.controller.api.mqtt;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

/**
 * This helper class wraps a connection to an MQTT broker using HiveMQ client.
 *
 * <p>
 * One main feature of this class is to retry the initial connection to an MQTT
 * broker with exponential backoff.
 */
public class MqttConnector {

	private static final int INCREASE_WAIT_SECONDS = 5;
	private static final int MAX_WAIT_SECONDS = 60 * 5;
	private final AtomicInteger waitSeconds = new AtomicInteger(0);

	private Mqtt5AsyncClient client;
	private CompletableFuture<Mqtt5AsyncClient> connectionFuture;

	protected synchronized void deactivate() {
		if (this.client != null) {
			try {
				this.client.disconnect().get(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				// ignore
			}
			this.client = null;
		}
	}

	/**
	 * Connects to the MQTT broker.
	 *
	 * @param serverUri      the broker URI (e.g., tcp://localhost:1883 or
	 *                       ssl://broker:8883)
	 * @param clientId       the client ID
	 * @param username       the username
	 * @param password       the password
	 * @param certPem        the client certificate PEM (optional)
	 * @param privateKeyPem  the private key PEM (optional)
	 * @param trustStorePem  the CA certificate PEM (optional)
	 * @return a {@link CompletableFuture} that completes with the connected client
	 */
	protected synchronized CompletableFuture<Mqtt5AsyncClient> connect(String serverUri, String clientId,
			String username, String password, String certPem, String privateKeyPem, String trustStorePem) {

		this.connectionFuture = new CompletableFuture<>();

		// Parse URI
		var uri = serverUri;
		var host = "localhost";
		var port = 1883;
		var useSsl = false;

		if (uri.startsWith("ssl://") || uri.startsWith("tls://")) {
			useSsl = true;
			port = 8883;
			uri = uri.substring(6);
		} else if (uri.startsWith("tcp://")) {
			uri = uri.substring(6);
		}

		var colonIndex = uri.indexOf(':');
		if (colonIndex > 0) {
			host = uri.substring(0, colonIndex);
			try {
				port = Integer.parseInt(uri.substring(colonIndex + 1));
			} catch (NumberFormatException e) {
				// use default
			}
		} else {
			host = uri;
		}

		// Build client
		var builder = MqttClient.builder() //
				.useMqttVersion5() //
				.identifier(clientId) //
				.serverHost(host) //
				.serverPort(port) //
				.automaticReconnect() //
				.initialDelay(INCREASE_WAIT_SECONDS, TimeUnit.SECONDS) //
				.maxDelay(MAX_WAIT_SECONDS, TimeUnit.SECONDS) //
				.applyAutomaticReconnect();

		// SSL configuration
		if (useSsl) {
			if (certPem != null && !certPem.isBlank() //
					&& privateKeyPem != null && !privateKeyPem.isBlank() //
					&& trustStorePem != null && !trustStorePem.isBlank()) {
				try {
					var sslConfig = createSslConfig(certPem, privateKeyPem, trustStorePem);
					builder.sslConfig(sslConfig);
				} catch (Exception e) {
					this.connectionFuture.completeExceptionally(
							new RuntimeException("Failed to configure SSL: " + e.getMessage(), e));
					return this.connectionFuture;
				}
			} else {
				builder.sslWithDefaultConfig();
			}
		}

		this.client = builder.buildAsync();

		// Build connect options
		var connectBuilder = this.client.connectWith() //
				.cleanStart(true) //
				.keepAlive(60);

		// Authentication
		if (username != null && !username.isBlank()) {
			connectBuilder.simpleAuth() //
					.username(username) //
					.password(password != null ? password.getBytes(StandardCharsets.UTF_8) : new byte[0]) //
					.applySimpleAuth();
		}

		// Connect
		connectBuilder.send() //
				.whenComplete((connAck, throwable) -> {
					if (throwable != null) {
						this.waitSeconds.getAndUpdate(
								oldValue -> Math.min(oldValue + INCREASE_WAIT_SECONDS, MAX_WAIT_SECONDS));
						this.connectionFuture.completeExceptionally(throwable);
					} else {
						this.waitSeconds.set(0);
						this.connectionFuture.complete(this.client);
					}
				});

		return this.connectionFuture;
	}

	/**
	 * Creates SSL configuration from PEM certificates.
	 */
	private static MqttClientSslConfig createSslConfig(String certPem, String privateKeyPem, String trustStorePem)
			throws Exception {
		// Load certificates
		var cf = CertificateFactory.getInstance("X.509");

		// Client certificate
		var clientCert = (X509Certificate) cf.generateCertificate(
				new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));

		// CA certificate
		var caCert = (X509Certificate) cf.generateCertificate(
				new ByteArrayInputStream(trustStorePem.getBytes(StandardCharsets.UTF_8)));

		// Private key
		var privateKey = loadPrivateKey(privateKeyPem);

		// Create KeyStore for client cert
		var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		keyStore.setKeyEntry("client", privateKey, new char[0], new X509Certificate[] { clientCert });

		var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, new char[0]);

		// Create TrustStore for CA
		var trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);
		trustStore.setCertificateEntry("ca", caCert);

		var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);

		return MqttClientSslConfig.builder() //
				.keyManagerFactory(kmf) //
				.trustManagerFactory(tmf) //
				.build();
	}

	/**
	 * Loads a private key from PEM format.
	 */
	private static PrivateKey loadPrivateKey(String pem) throws Exception {
		var pemContent = pem //
				.replace("-----BEGIN PRIVATE KEY-----", "") //
				.replace("-----END PRIVATE KEY-----", "") //
				.replace("-----BEGIN RSA PRIVATE KEY-----", "") //
				.replace("-----END RSA PRIVATE KEY-----", "") //
				.replaceAll("\\s", "");

		var decoded = Base64.getDecoder().decode(pemContent);
		var keySpec = new PKCS8EncodedKeySpec(decoded);

		// Try RSA first, then EC
		try {
			return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
		} catch (Exception e) {
			return KeyFactory.getInstance("EC").generatePrivate(keySpec);
		}
	}

	/**
	 * Gets the current client.
	 *
	 * @return the client or null
	 */
	public Mqtt5AsyncClient getClient() {
		return this.client;
	}

}
