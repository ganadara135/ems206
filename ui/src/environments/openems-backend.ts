import { Environment } from "../app/shared/type/environment";

export const environment: Environment = {
  production: true,
  debugMode: false,
  url: (location.protocol == "https:" ? "wss" : "ws") +
  //  "://" + location.hostname + ":" + location.port + "/openems-backend-ui2",
    "://" + "27.96.134.194" + ":" + "8082" + "/openems-backend-ui2",
  backend: "OpenEMS Backend",
};