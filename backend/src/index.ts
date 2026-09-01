import "./loadEnv";
import { createApp } from "./app";

const port = Number(process.env.PORT ?? 3700);
const host = process.env.HOST ?? "0.0.0.0";

const app = createApp();

app.listen(port, host, () => {
  console.log(`Credimax API escuchando en http://${host}:${port}`);
});
