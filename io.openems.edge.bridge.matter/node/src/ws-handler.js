/**
 * WebSocket JSON-RPC request dispatcher.
 *
 * Routes incoming JSON-RPC 2.0 requests to the appropriate handler method
 * and manages push notifications to connected clients.
 */

/**
 * Creates a new WS handler.
 *
 * @param {import('./matter-controller.js').MatterController} controller
 * @returns {object} handler with dispatch and notify methods
 */
export function createWsHandler(controller) {
  const subscribers = new Map(); // key: "nodeId:endpointId:clusterId" -> Set<WebSocket>

  /**
   * Dispatches a JSON-RPC request to the appropriate handler.
   *
   * @param {object} request - JSON-RPC 2.0 request
   * @param {import('ws').WebSocket} ws - the WebSocket client
   * @returns {Promise<object>} JSON-RPC 2.0 response
   */
  async function dispatch(request, ws) {
    const { id, method, params } = request;

    try {
      let result;

      switch (method) {
        case 'getDevices':
          result = await controller.getDevices();
          break;

        case 'commissionDevice':
          result = await controller.commissionDevice(params.pairingCode);
          break;

        case 'decommissionDevice':
          await controller.decommissionDevice(params.nodeId);
          result = { success: true };
          break;

        case 'subscribeAttributes': {
          const key = `${params.nodeId}:${params.endpointId}:${params.clusterId}`;
          if (!subscribers.has(key)) {
            subscribers.set(key, new Set());
          }
          subscribers.get(key).add(ws);

          await controller.subscribeAttributes(
            params.nodeId,
            params.endpointId,
            params.clusterId,
            params.attributeIds
          );
          result = { subscribed: true };
          break;
        }

        case 'readAttribute':
          result = await controller.readAttribute(
            params.nodeId,
            params.endpointId,
            params.clusterId,
            params.attributeId
          );
          break;

        default:
          return {
            jsonrpc: '2.0',
            id,
            error: { code: -32601, message: `Method not found: ${method}` },
          };
      }

      return { jsonrpc: '2.0', id, result };
    } catch (err) {
      return {
        jsonrpc: '2.0',
        id,
        error: { code: -32000, message: err.message },
      };
    }
  }

  /**
   * Sends a notification to all connected clients.
   *
   * @param {import('ws').WebSocketServer} wss
   * @param {string} method - notification method name
   * @param {object} params - notification parameters
   */
  function broadcast(wss, method, params) {
    const message = JSON.stringify({ jsonrpc: '2.0', method, params });
    for (const client of wss.clients) {
      if (client.readyState === 1 /* OPEN */) {
        client.send(message);
      }
    }
  }

  /**
   * Sends an attribute update notification to subscribed clients.
   *
   * @param {import('ws').WebSocketServer} wss
   * @param {object} update - { nodeId, endpointId, clusterId, attributeId, value }
   */
  function notifyAttributeUpdate(wss, update) {
    const key = `${update.nodeId}:${update.endpointId}:${update.clusterId}`;
    const subs = subscribers.get(key);

    // Broadcast to all connected clients (subscribed or not, for simplicity)
    broadcast(wss, 'attributeUpdate', update);
  }

  /**
   * Removes a WebSocket client from all subscriptions.
   *
   * @param {import('ws').WebSocket} ws
   */
  function removeClient(ws) {
    for (const clientSet of subscribers.values()) {
      clientSet.delete(ws);
    }
  }

  return {
    dispatch,
    broadcast,
    notifyAttributeUpdate,
    removeClient,
  };
}
