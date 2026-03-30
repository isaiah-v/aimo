import React from 'react';
import { Theme, useTheme } from '@mui/material/styles';

type VarAccessor = (theme: Theme) => string | number | undefined;

const DEFAULT_MAP: Record<string, VarAccessor> = {
  '--palette-primary-main': t => t.palette.primary?.main,
  '--palette-primary-contrastText': t => t.palette.primary?.contrastText,
  '--palette-secondary-main': t => t.palette.secondary?.main,
  '--palette-background-default': t => t.palette.background?.default,
  '--palette-background-paper': t => t.palette.background?.paper,
  '--palette-text-primary': t => t.palette.text?.primary,
  '--palette-text-secondary': t => t.palette.text?.secondary,
  '--font-family': t => (t.typography?.fontFamily as string) || undefined,
  '--spacing-1': t => {
    try { return String((t as any).spacing(1)); } catch { return undefined; }
  },
  '--spacing-2': t => {
    try { return String((t as any).spacing(2)); } catch { return undefined; }
  },
  '--shape-border-radius': t => `${t.shape.borderRadius as string}px`,
};

export default function ThemeCssVars({
  map = DEFAULT_MAP,
  scope = ':root',
}: {
  map?: Record<string, VarAccessor>;
  scope?: string;
}): null {
  const theme = useTheme();

  React.useEffect(() => {
    const root = document.querySelector(scope) || document.documentElement;
    const style = (root as HTMLElement).style;
    for (const [varName, accessor] of Object.entries(map)) {
      try {
        const value = accessor(theme);
        if (value === undefined || value === null) {
          // leave absent
        } else {
          style.setProperty(varName, String(value));
        }
      } catch (err) {
        // swallow errors
      }
    }
  }, [theme, map, scope]);

  return null;
}

