interface SquareBlockProps {
	size: number
	sign: '+' | '-'
	x?: number
	y?: number
	label?: string
	labelOffsetX?: number
	labelOffsetY?: number
	labelGap?: number
	labelWidth?: number
	labelFontSize?: number
	labelLineHeight?: number
	labelColor?: string
}

export function SquareBlock({
	size,
	sign,
	x = 0,
	y = 0,
	label,
	labelOffsetX = 0,
	labelOffsetY = 0,
	labelGap = 5,
	labelWidth = 140,
	labelFontSize = 13,
	labelLineHeight = 16,
	labelColor = '#000',
}: SquareBlockProps) {
	const circleSize = size * 0.6 // Размер круга относительно квадрата

	return (
		<div
			style={{
				position: 'absolute',
				left: x,
				top: y,
				width: size,
				height: size,
				border: '2px solid #000',
				backgroundColor: '#d3d3d3',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				overflow: 'visible',
			}}
		>
			{label ? (
				<div
					style={{
						position: 'absolute',
						left: size + labelGap + labelOffsetX,
						top: labelOffsetY,
						width: labelWidth,
						fontSize: labelFontSize,
						lineHeight: `${labelLineHeight}px`,
						color: labelColor,
						whiteSpace: 'pre-line',
						userSelect: 'none',
					}}
				>
					{label}
				</div>
			) : null}

			{/* Белый круг */}
			<div
				style={{
					width: circleSize,
					height: circleSize,
					borderRadius: '50%',
					backgroundColor: '#fff',
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					position: 'relative',
				}}
			>
				{/* Знак + или - */}
				{sign === '+' ? (
					<svg
						width={circleSize * 0.5}
						height={circleSize * 0.5}
						viewBox="0 0 24 24"
						style={{ display: 'block' }}
					>
						<line
							x1="12"
							y1="6"
							x2="12"
							y2="18"
							stroke="#000"
							strokeWidth="3"
							strokeLinecap="round"
						/>
						<line
							x1="6"
							y1="12"
							x2="18"
							y2="12"
							stroke="#000"
							strokeWidth="3"
							strokeLinecap="round"
						/>
					</svg>
				) : (
					<svg
						width={circleSize * 0.5}
						height={circleSize * 0.5}
						viewBox="0 0 24 24"
						style={{ display: 'block' }}
					>
						<line
							x1="6"
							y1="12"
							x2="18"
							y2="12"
							stroke="#000"
							strokeWidth="3"
							strokeLinecap="round"
						/>
					</svg>
				)}
			</div>
		</div>
	)
}

