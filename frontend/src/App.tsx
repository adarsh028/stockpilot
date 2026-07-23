import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./layouts/AppLayout";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import Login from "./pages/auth/Login";
import Signup from "./pages/auth/Signup";
import VerifyOtp from "./pages/auth/VerifyOtp";
import ForgotPassword from "./pages/auth/ForgotPassword";
import ResetPassword from "./pages/auth/ResetPassword";
import Dashboard from "./pages/Dashboard";
import ProductsLayout from "./pages/products/ProductsLayout";
import ProductList from "./pages/products/ProductList";
import ProductForm from "./pages/products/ProductForm";
import ProductImport from "./pages/products/ProductImport";
import InventoryList from "./pages/inventory/InventoryList";
import ChannelListings from "./pages/inventory/ChannelListings";
import ChannelList from "./pages/channels/ChannelList";
import SalesLayout from "./pages/sales/SalesLayout";
import SalesList from "./pages/sales/SalesList";
import SalesImport from "./pages/sales/SalesImport";
import ImportBatches from "./pages/sales/ImportBatches";
import TeamList from "./pages/team/TeamList";
import SettingsLayout from "./pages/settings/SettingsLayout";
import SettingsGeneral from "./pages/settings/SettingsGeneral";
import SettingsIntegrations from "./pages/settings/SettingsIntegrations";
import Categories from "./pages/settings/Categories";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/verify-otp" element={<VerifyOtp />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Dashboard />} />

        {/* Products — catalog and importer are tabs of one section. */}
        <Route path="/products" element={<ProductsLayout />}>
          <Route index element={<ProductList />} />
          <Route path="import" element={<ProductImport />} />
        </Route>
        {/* Product create/edit are focused full-page forms, outside the tab shell. */}
        <Route path="/products/new" element={<ProductForm />} />
        <Route path="/products/:id/edit" element={<ProductForm />} />

        <Route path="/inventory" element={<InventoryList />} />
        <Route path="/channels/:channelId/listings" element={<ChannelListings />} />
        <Route path="/channels" element={<ChannelList />} />

        {/* Sales — orders, import and history are tabs of one section. */}
        <Route path="/sales" element={<SalesLayout />}>
          <Route index element={<SalesList />} />
          <Route path="import" element={<SalesImport />} />
          <Route path="batches" element={<ImportBatches />} />
        </Route>

        <Route
          path="/team"
          element={
            <ProtectedRoute roles={["OWNER", "ADMIN"]}>
              <TeamList />
            </ProtectedRoute>
          }
        />

        {/* Settings — general, integrations and categories are tabs of one section. */}
        <Route path="/settings" element={<SettingsLayout />}>
          <Route index element={<SettingsGeneral />} />
          <Route path="integrations" element={<SettingsIntegrations />} />
          <Route
            path="categories"
            element={
              <ProtectedRoute roles={["OWNER", "ADMIN"]}>
                <Categories />
              </ProtectedRoute>
            }
          />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
