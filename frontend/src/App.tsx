import { Navigate, Route, Routes } from "react-router-dom";
import { OperatorProtectedLayout, ProtectedFlowLayout, ProtectedLayout } from "./components/layout/ProtectedLayout";
import { SplashPage } from "./pages/SplashPage";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { HomePage } from "./pages/HomePage";
import { ExchangePage } from "./pages/ExchangePage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { MyPage } from "./pages/MyPage";
import { ShipmentsPage } from "./pages/ShipmentsPage";
import { CapacitySearchPage } from "./pages/CapacitySearchPage";
import { ConsolidationDetailPage } from "./pages/ConsolidationDetailPage";
import { CargoRegisterPage } from "./pages/cargo/CargoRegisterPage";
import { CargoGatewayPage } from "./pages/cargo/CargoGatewayPage";
import { CargoAnalysisPage } from "./pages/cargo/CargoAnalysisPage";
import { CargoRecommendationsPage } from "./pages/cargo/CargoRecommendationsPage";
import { CargoCheckoutPage } from "./pages/cargo/CargoCheckoutPage";
import { CargoStatusPage } from "./pages/cargo/CargoStatusPage";
import { ModeRecommendationPage } from "./pages/cargo/ModeRecommendationPage";
import { ModeComparisonPage } from "./pages/cargo/ModeComparisonPage";
import { DoorToDoorPage } from "./pages/cargo/DoorToDoorPage";
import { ReservationConfirmPage } from "./pages/cargo/ReservationConfirmPage";
import { IntegratedReservationPage } from "./pages/cargo/IntegratedReservationPage";
import { RoadQuotePage } from "./pages/cargo/RoadQuotePage";
import { OperatorDashboardPage } from "./pages/OperatorDashboardPage";
import { ScrollToTop } from "./components/layout/ScrollToTop";

function App() {
  return (
    <>
    <ScrollToTop />
    <Routes>
      <Route path="/" element={<Navigate to="/splash" replace />} />
      <Route path="/splash" element={<SplashPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />

      <Route element={<ProtectedLayout />}>
        <Route path="/home" element={<HomePage />} />
        <Route path="/exchange" element={<ExchangePage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/shipments" element={<ShipmentsPage />} />
        <Route path="/me" element={<MyPage />} />
      </Route>

      <Route element={<ProtectedFlowLayout />}>
        <Route path="/capacity" element={<CapacitySearchPage />} />
        <Route path="/consolidated-cargos/:groupId" element={<ConsolidationDetailPage />} />
        <Route path="/cargo/new" element={<CargoGatewayPage />} />
        <Route path="/cargo/new/form" element={<CargoRegisterPage />} />
        <Route path="/cargo/:cargoId/analysis" element={<CargoAnalysisPage />} />
        <Route path="/cargo/:cargoId/mode-recommendation" element={<ModeRecommendationPage />} />
        <Route path="/cargo/:cargoId/mode-comparison" element={<ModeComparisonPage />} />
        <Route path="/cargo/:cargoId/recommendations" element={<CargoRecommendationsPage />} />
        <Route path="/cargo/:cargoId/door-to-door/:groupId" element={<DoorToDoorPage />} />
        <Route path="/cargo/:cargoId/reservation/:groupId" element={<ReservationConfirmPage />} />
        <Route path="/cargo/:cargoId/integrated-reservation/:groupId" element={<IntegratedReservationPage />} />
        <Route path="/cargo/:cargoId/road-quote" element={<RoadQuotePage />} />
        <Route path="/cargo/:cargoId/checkout/:groupId" element={<CargoCheckoutPage />} />
        <Route path="/cargo/:cargoId/status" element={<CargoStatusPage />} />
      </Route>

      <Route element={<OperatorProtectedLayout />}>
        <Route path="/operator" element={<OperatorDashboardPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/splash" replace />} />
    </Routes>
    </>
  );
}

export default App;
