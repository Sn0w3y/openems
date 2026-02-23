/**
 * matter.js CommissioningController wrapper.
 *
 * Manages the Matter fabric, device commissioning, attribute subscriptions
 * and reads.
 */

import { Environment, StorageService } from '@matter/main';
import {
  CommissioningController,
  NodeStates,
} from '@matter/main/protocol';

/**
 * Creates a new MatterController.
 *
 * @param {string} storagePath - path to persist fabric/device state
 * @param {function} onAttributeUpdate - callback for attribute changes
 * @param {function} onDeviceStateChange - callback for device state changes
 * @returns {Promise<object>} controller API
 */
export async function createMatterController(storagePath, onAttributeUpdate, onDeviceStateChange) {
  const environment = Environment.default;

  // Configure storage location
  const storageService = environment.get(StorageService);
  storageService.location = storagePath;

  // Create the commissioning controller
  const controller = new CommissioningController({
    environment,
    autoConnect: true,
  });

  await controller.start();

  /**
   * Gets all commissioned devices.
   *
   * @returns {Array<object>} list of device info objects
   */
  async function getDevices() {
    const devices = [];
    const nodes = controller.getCommissionedNodes();

    for (const nodeId of nodes) {
      try {
        const node = controller.getNode(nodeId);
        const basicInfo = await getBasicInfo(node);
        devices.push({
          nodeId: nodeId.toString(),
          vendorName: basicInfo.vendorName || 'Unknown',
          productName: basicInfo.productName || 'Unknown',
          serialNumber: basicInfo.serialNumber || null,
          endpoints: getEndpointIds(node),
        });
      } catch (err) {
        console.error(`Failed to get info for node ${nodeId}: ${err.message}`);
        devices.push({
          nodeId: nodeId.toString(),
          vendorName: 'Unknown',
          productName: 'Unknown',
          serialNumber: null,
          endpoints: [],
        });
      }
    }

    return devices;
  }

  /**
   * Commissions a new device.
   *
   * @param {string} pairingCode - Matter pairing code
   * @returns {Promise<object>} device info
   */
  async function commissionDevice(pairingCode) {
    const node = await controller.commissionNode({
      discovery: {
        identifierData: { longDiscriminator: undefined, shortDiscriminator: undefined },
      },
      passcode: undefined,
      commissioning: {
        regulatoryLocation: 0, // Indoor
        regulatoryCountryCode: 'XX',
      },
      pairingCode,
    });

    const nodeId = node.nodeId;
    const basicInfo = await getBasicInfo(node);

    return {
      nodeId: nodeId.toString(),
      vendorName: basicInfo.vendorName || 'Unknown',
      productName: basicInfo.productName || 'Unknown',
      serialNumber: basicInfo.serialNumber || null,
      endpoints: getEndpointIds(node),
    };
  }

  /**
   * Decommissions (removes) a device.
   *
   * @param {string|number} nodeId
   */
  async function decommissionDevice(nodeId) {
    const node = controller.getNode(BigInt(nodeId));
    await controller.removeNode(node.nodeId);
  }

  /**
   * Subscribes to attribute updates.
   *
   * @param {string|number} nodeId
   * @param {number} endpointId
   * @param {number} clusterId
   * @param {Array<number>} attributeIds
   */
  async function subscribeAttributes(nodeId, endpointId, clusterId, attributeIds) {
    const node = controller.getNode(BigInt(nodeId));

    // Register event listener for attribute changes
    node.events.attributeChanged.on((data) => {
      if (
        data.path.endpointId === endpointId &&
        data.path.clusterId === clusterId &&
        attributeIds.includes(data.path.attributeId)
      ) {
        onAttributeUpdate({
          nodeId: nodeId.toString(),
          endpointId: data.path.endpointId,
          clusterId: data.path.clusterId,
          attributeId: data.path.attributeId,
          value: data.value,
        });
      }
    });

    // Also register state change notifications
    node.events.stateChanged.on((state) => {
      onDeviceStateChange({
        nodeId: nodeId.toString(),
        state: NodeStates[state] || String(state),
      });
    });
  }

  /**
   * Reads a single attribute.
   *
   * @param {string|number} nodeId
   * @param {number} endpointId
   * @param {number} clusterId
   * @param {number} attributeId
   * @returns {Promise<object>} attribute value
   */
  async function readAttribute(nodeId, endpointId, clusterId, attributeId) {
    const node = controller.getNode(BigInt(nodeId));

    // Read attributes via the interaction client
    const interactionClient = await node.getInteractionClient();
    const result = await interactionClient.getAttribute({
      endpointId,
      clusterId,
      attributeId,
    });

    return { value: result };
  }

  /**
   * Gets basic information from a node's root endpoint.
   */
  async function getBasicInfo(node) {
    try {
      const interactionClient = await node.getInteractionClient();
      // Basic Information Cluster = 0x0028
      const vendorName = await interactionClient.getAttribute({
        endpointId: 0,
        clusterId: 0x0028,
        attributeId: 1, // VendorName
      });
      const productName = await interactionClient.getAttribute({
        endpointId: 0,
        clusterId: 0x0028,
        attributeId: 3, // ProductName
      });
      const serialNumber = await interactionClient.getAttribute({
        endpointId: 0,
        clusterId: 0x0028,
        attributeId: 15, // SerialNumber
      });
      return { vendorName, productName, serialNumber };
    } catch {
      return { vendorName: 'Unknown', productName: 'Unknown', serialNumber: null };
    }
  }

  /**
   * Gets endpoint IDs from a node.
   */
  function getEndpointIds(node) {
    try {
      const endpoints = node.getDevices();
      return endpoints.map((ep) => ep.number);
    } catch {
      return [];
    }
  }

  /**
   * Shuts down the controller.
   */
  async function close() {
    await controller.close();
  }

  return {
    getDevices,
    commissionDevice,
    decommissionDevice,
    subscribeAttributes,
    readAttribute,
    close,
  };
}
