import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { HashRouter } from "react-router";
import {
  QueryClient,
  QueryClientProvider,
  QueryCache,
  MutationCache,
} from "@tanstack/react-query";
import { SessionProvider } from "./features/session/session";
import { TooltipProvider } from "./components/ui/tooltip";
import { isSessionError } from "./lib/api";
import { App } from "./app/app";
import "./styles.css";
const onError = (error: unknown) => {
  if (isSessionError(error))
    window.dispatchEvent(new Event("swarmpit:session-expired"));
};
const client = new QueryClient({
  queryCache: new QueryCache({ onError }),
  mutationCache: new MutationCache({ onError }),
  defaultOptions: {
    queries: { staleTime: 5000, refetchOnWindowFocus: true },
    mutations: { retry: false },
  },
});
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={client}>
      <HashRouter>
        <SessionProvider>
          <TooltipProvider>
            <App />
          </TooltipProvider>
        </SessionProvider>
      </HashRouter>
    </QueryClientProvider>
  </StrictMode>,
);
