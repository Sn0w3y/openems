/**
 * OpenEMS Matter Server - Entry point.
 *
 * Starts a WebSocket server and initializes the matter.js
 * CommissioningController. Communicates with the Java Bridge via JSON-RPC 2.0.
 *
 * CLI args:
 *   --port <number>    WebSocket server port (0 = random)
 *   --storage <path>   Path for Matter data persistence
 */

import { WebSocketServer } from 'ws';
import { createMatterController } from './matter-controller.js';
import { createWsHandler } from './ws-handler.js';

// Parse CLI arguments
const args = process.argv.slice(2);
function getArg(name, defaultValue) {
  const idx = args.indexOf(`--${name}`);
  return idx !== -1 && idx + 1 < args.length ? args[idx + 1] : defaultValue;
}

const port = parseInt(getArg('port', '0'), 10);
const storagePath = getArg('storage', './data');

async function main() {
  console.error(`[matter-server] Starting with port=${port}, storage=${storagePath}`);

  // Create WebSocket server
  const wss = new WebSocketServer({ port, host: '127.0.0.1' });

  // Create matter.js controller
  let handler;

  const controller = await createMatterController(
    storagePath,
    // onAttributeUpdate
    (update) => {
      if (handler) {
        handler.notifyAttributeUpdate(wss, update);
      }
    },
    // onDeviceStateChange
    (change) => {
      if (handler) {
        handler.broadcast(wss, 'deviceStateChange', change);
      }
    }
  );

  handler = createWsHandler(controller);

  // Handle new WebSocket connections
  wss.on('connection', (ws) => {
    console.error('[matter-server] Client connected');

    ws.on('message', async (data) => {
      let request;
      try {
        request = JSON.parse(data.toString());
      } catch {
        ws.send(
          JSON.stringify({
            jsonrpc: '2.0',
            id: null,
            error: { code: -32700, message: 'Parse error' },
          })
        );
        return;
      }

      const response = await handler.dispatch(request, ws);
      ws.send(JSON.stringify(response));
    });

    ws.on('close', () => {
      console.error('[matter-server] Client disconnected');
      handler.removeClient(ws);
    });

    ws.on('error', (err) => {
      console.error(`[matter-server] WebSocket error: ${err.message}`);
    });
  });

  // Announce the actual listening port to Java via stdout (JSON format)
  const address = wss.address();
  const actualPort = address.port;

  // This line is parsed by NodeJsProcessManager to discover the port
  console.log(JSON.stringify({ port: actualPort }));

  // Notify readiness
  console.error(`[matter-server] WebSocket server listening on 127.0.0.1:${actualPort}`);
  handler.broadcast(wss, 'ready', { port: actualPort });

  // Handle shutdown signals
  const shutdown = async () => {
    console.error('[matter-server] Shutting down...');
    wss.close();
    await controller.close();
    process.exit(0);
  };

  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}

main().catch((err) => {
  console.error(`[matter-server] Fatal error: ${err.message}`);
  console.error(err.stack);
  process.exit(1);
});
