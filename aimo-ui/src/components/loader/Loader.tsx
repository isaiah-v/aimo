import React, { useState, useEffect } from 'react';
import {useTheme} from "@mui/material/styles";


export interface LoaderProps {
    width?: number; // base size in px
    visible?: boolean;
}

const Loader: React.FC<LoaderProps> = ({ width = 50, visible = true }) => {
    const theme = useTheme();
    const fillColor = theme.palette.text.primary

    const [show, setShow] = useState(visible);

    useEffect(() => {
        setShow(visible);
    }, [visible]);

    if (!show) return null;

    const containerStyle:  React.CSSProperties = {
        display: 'inline-flex',
        alignItems: 'center',
        gap: 8,
        userSelect: 'none',
        backgroundColor: 'transparent',
    };


    return (
        <svg style={containerStyle} width={width} height={0.30 * width} viewBox="0 0 100 30">
            <circle cx="15" cy="15" r="10" stroke="green" strokeWidth="0" fill={fillColor}>
                <animate attributeName="opacity" values="0;1;0" dur="1.2s" begin="0s" repeatCount="indefinite" />
            </circle>
            <circle cx="50" cy="15" r="10" stroke="green" strokeWidth="0" fill={fillColor}>
                <animate attributeName="opacity" values="0;1;0" dur="1.2s" begin="0.2s" repeatCount="indefinite" />
            </circle>
            <circle cx="85" cy="15" r="10" stroke="green" strokeWidth="0" fill={fillColor}>
                <animate attributeName="opacity" values="0;1;0" dur="1.2s" begin="0.4s" repeatCount="indefinite" />
            </circle>
        </svg>
    );
};

export default Loader;