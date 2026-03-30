import React from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'
import { ThemeProvider } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
import ThemeCssVars from './theme/ThemeCssVars'
import makeTheme from './theme/makeTheme'

const theme = makeTheme(window.matchMedia("(prefers-color-scheme: dark)").matches ? 'dark' : 'light')

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ThemeCssVars />
      <App />
    </ThemeProvider>
  </React.StrictMode>,
)
