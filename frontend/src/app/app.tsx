import { Routes, Route } from "react-router";
import { Shell } from "./shell";
import { RequireSession, Login } from "@/features/session/session";
import { Dashboard } from "@/features/dashboard/dashboard";
import {
  ServicesList,
  StacksList,
  NodesList,
  TasksList,
  NetworksList,
  VolumesList,
  SecretsList,
  ConfigsList,
  UsersList,
} from "@/features/resources/lists";
import {
  NodeDetail,
  TaskDetail,
  NetworkDetail,
  VolumeDetail,
  SecretDetail,
  ConfigDetail,
} from "@/features/resources/detail";
import {
  NetworkForm,
  VolumeForm,
  ContentForm,
  NodeForm,
} from "@/features/resources/forms";
import { ServiceForm } from "@/features/services/service-form";
import { ImagePicker } from "@/features/services/image-picker";
import { ServiceDetail } from "@/features/services/detail";
import { StackForm } from "@/features/stacks/stack-form";
import { StackDetail } from "@/features/stacks/detail";
import {
  RegistriesList,
  RegistryDetail,
  RegistryForm,
} from "@/features/registries/registries";
import { UserDetail, UserForm } from "@/features/users/users";
import { Account } from "@/features/session/account";
import { Page } from "@/components/common";
export function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        element={
          <RequireSession fallback={<Shell pending />}>
            <Shell />
          </RequireSession>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="/services" element={<ServicesList />} />
        <Route path="/services/create/wizard/image" element={<ImagePicker />} />
        <Route
          path="/services/create/wizard/config"
          element={<ServiceForm />}
        />
        <Route path="/services/:id" element={<ServiceDetail />} />
        <Route path="/services/:id/edit" element={<ServiceForm />} />
        <Route path="/stacks" element={<StacksList />} />
        <Route path="/stacks/create" element={<StackForm />} />
        <Route path="/stacks/:name" element={<StackDetail />} />
        {["compose", "previous", "last", "activate"].map((action) => (
          <Route
            key={action}
            path={"/stacks/:name/" + action}
            element={<StackForm />}
          />
        ))}
        <Route path="/nodes" element={<NodesList />} />
        <Route path="/nodes/:id" element={<NodeDetail />} />
        <Route path="/nodes/:id/edit" element={<NodeForm />} />
        <Route path="/tasks" element={<TasksList />} />
        <Route path="/tasks/:id" element={<TaskDetail />} />
        <Route path="/networks" element={<NetworksList />} />
        <Route path="/networks/create" element={<NetworkForm />} />
        <Route path="/networks/:id" element={<NetworkDetail />} />
        <Route path="/volumes" element={<VolumesList />} />
        <Route path="/volumes/create" element={<VolumeForm />} />
        <Route path="/volumes/:name" element={<VolumeDetail />} />
        <Route path="/secrets" element={<SecretsList />} />
        <Route
          path="/secrets/create"
          element={<ContentForm kind="secrets" />}
        />
        <Route path="/secrets/:id" element={<SecretDetail />} />
        <Route path="/configs" element={<ConfigsList />} />
        <Route
          path="/configs/create"
          element={<ContentForm kind="configs" />}
        />
        <Route path="/configs/:id" element={<ConfigDetail />} />
        <Route path="/registries" element={<RegistriesList />} />
        <Route path="/registries/create" element={<RegistryForm />} />
        <Route
          path="/registries/:registryType/:id"
          element={<RegistryDetail />}
        />
        <Route
          path="/registries/:registryType/:id/edit"
          element={<RegistryForm />}
        />
        <Route path="/users" element={<UsersList />} />
        <Route path="/users/create" element={<UserForm />} />
        <Route path="/users/:id" element={<UserDetail />} />
        <Route path="/users/:id/edit" element={<UserForm />} />
        <Route path="/account-settings" element={<Account />} />
        <Route
          path="/unauthorized"
          element={
            <Page title="Access restricted">
              <p>
                Your account does not have permission to access this resource.
              </p>
            </Page>
          }
        />
        <Route
          path="/error"
          element={
            <Page title="Request failed">
              <p>Return to the resource and try again.</p>
            </Page>
          }
        />
        <Route
          path="*"
          element={
            <Page title="Page not found">
              <p>This page does not exist.</p>
            </Page>
          }
        />
      </Route>
    </Routes>
  );
}
