import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./layouts/AppLayout";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import Login from "./pages/auth/Login";
import Signup from "./pages/auth/Signup";
import VerifyOtp from "./pages/auth/VerifyOtp";
import ForgotPassword from "./pages/auth/ForgotPassword";
import ResetPassword from "./pages/auth/ResetPassword";
import Dashboard from "./pages/Dashboard";
import ProductList from "./pages/products/ProductList";
import ProductForm from "./pages/products/ProductForm";
import ProductImport from "./pages/products/ProductImport";
import InventoryList from "./pages/inventory/InventoryList";
import ChannelListings from "./pages/inventory/ChannelListings";
import ChannelList from "./pages/channels/ChannelList";
import SalesList from "./pages/sales/SalesList";
import SalesImport from "./pages/sales/SalesImport";
import ImportBatches from "./pages/sales/ImportBatches";
import TeamList from "./pages/team/TeamList";
import Settings from "./pages/settings/Settings";
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
        <Route path="/products" element={<ProductList />} />
        <Route path="/products/new" element={<ProductForm />} />
        <Route path="/products/:id/edit" element={<ProductForm />} />
        <Route path="/products/import" element={<ProductImport />} />
        <Route path="/inventory" element={<InventoryList />} />
        <Route path="/channels/:channelId/listings" element={<ChannelListings />} />
        <Route path="/channels" element={<ChannelList />} />
        <Route path="/sales" element={<SalesList />} />
        <Route path="/sales/import" element={<SalesImport />} />
        <Route path="/sales/batches" element={<ImportBatches />} />
        <Route
          path="/team"
          element={
            <ProtectedRoute roles={["OWNER", "ADMIN"]}>
              <TeamList />
            </ProtectedRoute>
          }
        />
        <Route path="/settings" element={<Settings />} />
        <Route
          path="/settings/categories"
          element={
            <ProtectedRoute roles={["OWNER", "ADMIN"]}>
              <Categories />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
