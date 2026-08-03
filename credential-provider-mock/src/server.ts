import { buildApp } from "./app.js";
import { loadConfig } from "./config.js";

const config = loadConfig();
const app = await buildApp(config);

try {
  await app.listen({ host: config.host, port: config.port });
  process.stdout.write(`Credential provider mock listening on port ${config.port}\n`);
} catch (error) {
  process.stderr.write("Credential provider mock could not start\n");
  process.exitCode = 1;
}
