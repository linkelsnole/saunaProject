import React from 'react'

interface DiagonalBlockProps {
	size: number
	x?: number
	y?: number
	// true - открыто (зеленый), false/null - закрыто (серый)
	isOpen?: boolean | null
}

export function DiagonalBlock({ size, x = 0, y = 0, isOpen = null }: DiagonalBlockProps) {
	const circleSize = size * 0.3

	const indicatorColor = isOpen === true ? '#10B981' : '#94a3b8'

	const indicatorShadow = isOpen === true
		? '0 0 10px 2px rgba(16, 185, 129, 0.4), inset 0 2px 4px rgba(255,255,255,0.4)'
		: 'inset 0 2px 4px rgba(0, 0, 0, 0.2)'
	const indicatorBorder = isOpen === true ? '2px solid #34D399' : `2px solid ${indicatorColor}`


	return (
		<div
			style={{
				position: 'absolute',
				left: x,
				top: y,
				width: size,
				height: size,
				backgroundColor: '#FFFFFF',
				border: '1px solid #E2E8F0', 
				borderRadius: '14px', 
				boxShadow: '0 4px 12px -2px rgba(0, 0, 0, 0.08), 0 2px 6px -1px rgba(0, 0, 0, 0.04)',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				overflow: 'hidden', 
				zIndex: 10,
			}}
		>
			{/* Фоновая диагональная линия (SVG для четкости) */}
			<svg
				width={size}
				height={size}
				style={{ position: 'absolute', top: 0, left: 0 }}
			>
				<line
					x1="0"
					y1={size}
					x2={size}
					y2="0"
					stroke="#CBD5E1" // Цвет линии: Slate-300 (не черный, а приятный серый)
					strokeWidth="2"  // Толщина 2px
					strokeLinecap="round"
				/>
			</svg>

			{/* Центральный индикатор (HTML div для крутых CSS теней) */}
			<div
				style={{
					width: circleSize,
					height: circleSize,
					borderRadius: '50%',
					backgroundColor: indicatorColor,
					// Применяем магию теней и границ
					boxShadow: indicatorShadow,
					border: indicatorBorder,
					// Небольшая белая обводка снаружи, чтобы отделить от линии
					outline: '2px solid white',
					zIndex: 2, // Поверх линии
					transition: 'all 0.3s ease', // Плавное переключение статуса
				}}
			/>
		</div>
	)
}