import { Group, Circle, Text } from 'react-konva'

interface DeltaPBlockProps {
	x: number
	y: number
	radius?: number
	text?: string
	fontSize?: number
	strokeWidth?: number
	strokeColor?: string
	fillColor?: string
	/**
	 * Optional floating label (e.g. values) placed on the left/right.
	 * Supports multi-line text via `\n`.
	 */
	label?: string
	labelSide?: 'left' | 'right'
	labelGap?: number
	labelWidth?: number
	labelOffsetX?: number
	labelOffsetY?: number
	labelFontSize?: number
	labelLineHeight?: number
	labelColor?: string
	labelAlign?: 'left' | 'center' | 'right'
}

export function DeltaPBlock({
	x,
	y,
	radius = 18,
	text = 'ΔP2',
	fontSize = 14,
	strokeWidth = 2,
	strokeColor = '#000',
	fillColor = '#fff',
	label,
	labelSide = 'left',
	labelGap = 10,
	labelWidth = 140,
	labelOffsetX = 0,
	labelOffsetY = 0,
	labelFontSize = 14,
	labelLineHeight = 16,
	labelColor = '#000',
	labelAlign,
}: DeltaPBlockProps) {
	const isLabelLeft = labelSide === 'left'
	const labelX = isLabelLeft
		? -labelGap - labelWidth + labelOffsetX
		: radius * 2 + labelGap + labelOffsetX
	const effectiveLabelAlign = labelAlign ?? (isLabelLeft ? 'right' : 'left')

	return (
		<Group x={x} y={y}>
			<Circle
				x={0}
				y={0}
				radius={radius}
				stroke={strokeColor}
				strokeWidth={strokeWidth}
				fill={fillColor}
			/>
			<Text
				text={text}
				x={-radius}
				y={-radius}
				width={radius * 2}
				height={radius * 2}
				align="center"
				verticalAlign="middle"
				fontSize={fontSize}
				fill={strokeColor}
			/>

			{label ? (
				<Text
					text={label}
					x={labelX}
					y={-labelFontSize / 2 + labelOffsetY}
					width={labelWidth}
					align={effectiveLabelAlign}
					fontSize={labelFontSize}
					lineHeight={labelLineHeight / labelFontSize}
					fill={labelColor}
				/>
			) : null}
		</Group>
	)
}

