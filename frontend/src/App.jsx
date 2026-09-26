import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { PlantProvider } from './context/PlantContext';
import { AppRoutes } from './routes/AppRoutes';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <PlantProvider>
          <AppRoutes />
        </PlantProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
