import React, { useState, useEffect } from 'react';
import {Alert as MuiAlert, Fade} from '@mui/material';
import Stack from '@mui/material/Stack';
import {useTheme} from "@mui/material/styles";
import {alertService, Alert} from "../../services/alert-service/AlertService";

interface ErrorStackProps {}

interface FadeAlert {
    alert: Alert;
    fadeOut: boolean;
    timestamp?: number;
}

const fadeOutDuration = 500; // milliseconds

export const AlertStack: React.FC<ErrorStackProps> = (props: ErrorStackProps) => {
    const [ alerts, setAlerts ] = useState<FadeAlert[]>([]);

    useEffect(() => {
        return alertService.subscribe ((newAlerts: Alert[]) => {
            setAlerts(prev => {
                const now = Date.now();

                const newKeys = new Set(newAlerts.map(a => a.id));

                // mark previous alerts that are missing in newAlerts to fade out
                const updatedPrev = prev
                    .filter(a => now-a.timestamp! < fadeOutDuration || !a.fadeOut) // remove fully faded out alerts
                    .map(fa => {
                        const key = fa.alert.id;
                        if (!newKeys.has(key) && !fa.fadeOut) {
                            return { ...fa, fadeOut: true, timestamp: now };
                        }
                        return fa;
                    })

                // add incoming alerts that are not already in prev
                const prevKeys = new Set(prev.map(fa => fa.alert.id));
                const toAdd = newAlerts
                    .filter(na => !prevKeys.has(na.id))
                    .map(na => ({ alert: na, fadeOut: false }))
                    .reverse()

                return [...toAdd, ...updatedPrev];
            });
        })
    }, []);

    const theme = useTheme()

    return (
        <Stack
            sx={{
                top: theme.mixins.toolbar.minHeight,
                position: 'absolute',
            }}
        >
            {alerts.map((fa, idx) => (
                <Fade
                    key={fa.alert.id}
                    in={!fa.fadeOut}
                    timeout={{
                        exit: fadeOutDuration,
                    }}
                    unmountOnExit
                    appear={false}

                >
                    <div>
                        <MuiAlert
                            severity={fa.alert.level}
                            onClick={(e) => e.stopPropagation()}
                            sx={{ mb: 1 }}
                        >
                            {fa.alert.message}
                        </MuiAlert>
                    </div>
                </Fade>
            ))}
        </Stack>
    )
}