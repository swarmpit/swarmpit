import type { User } from "@/lib/types";
export interface UserValues {
  username: string;
  email: string;
  role: User["role"];
  password: string;
}
export function userPayload(
  values: UserValues,
  existing?: User,
): Record<string, unknown> {
  if (!existing) return { ...values };
  const { password: _password, ...safe } = existing as User & {
    password?: string;
  };
  return { ...safe, email: values.email, role: values.role };
}
