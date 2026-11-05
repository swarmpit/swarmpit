import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api, type ApiOptions, ApiError } from "./api";
export const apiKey = (path: string) => ["api", path] as const;
export function useApi<T>(
  path: string,
  enabled = true,
  refetchInterval?: number,
) {
  return useQuery({
    queryKey: apiKey(path),
    queryFn: ({ signal }) => api<T>(path, { signal }),
    enabled,
    refetchInterval,
    retry: (count, e) =>
      !(e instanceof ApiError && e.status >= 400 && e.status < 500) &&
      count < 1,
  });
}
export function useCommand<T = unknown>(onSuccess?: (data: T) => void) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ path, ...options }: ApiOptions & { path: string }) =>
      api<T>(path, { method: "POST", ...options }),
    retry: false,
    onSuccess: async (data) => {
      await client.invalidateQueries({ queryKey: ["api"] });
      onSuccess?.(data);
    },
  });
}
