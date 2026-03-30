import { createTheme, darken, lighten } from '@mui/material/styles';

/**
 * Create a theme that derives `background.paper` from `background.default` so
 * surfaces have a small, consistent contrast in both light and dark modes.
 *
 * @param mode 'light' | 'dark'
 */
export default function makeTheme(mode: 'light' | 'dark' = 'light') {
  // base theme so palette tokens are populated
  const base = createTheme({ palette: { mode } });

  // safe fallback for default background
  const defaultBg = base.palette.background.default ?? (mode === 'dark' ? '#121212' : '#fafafa');

  const paper =
    mode === 'dark'
      ? lighten(defaultBg, 0.06) // slightly lighter in dark mode
      : darken(defaultBg, 0.02); // slightly darker in light mode

  return createTheme({
    ...base,
    palette: {
      ...base.palette,
      background: {
        ...base.palette.background,
        default: defaultBg,
        paper,
      },
    },
  });
}

