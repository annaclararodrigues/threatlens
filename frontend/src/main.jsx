import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { applyTheme, isStoredThemeLight } from './utils/theme'

// Antes do primeiro render: login/404 respeitam o tema salvo e não há "piscar" escuro
applyTheme(isStoredThemeLight())

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
