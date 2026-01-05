import { Group, Circle, Line, Text } from 'react-konva'
import { Html } from 'react-konva-utils'

interface TriangleBlockProps {
	size: number
	x?: number
	y?: number
	rotation?: number
	label?: string
	leftLabel?: string
	rightLabel?: string
	labelSide?: 'left' | 'right'
	labelOffsetX?: number
	rightLabelOffsetX?: number
	labelOffsetY?: number
	labelGap?: number
	labelWidth?: number
	labelFontSize?: number
	labelLineHeight?: number
	labelColor?: string
	labelAlign?: 'left' | 'center' | 'right'
}

export function TriangleBlock({
	size,
	x = 0,
	y = 0,
	rotation = 0,
	label,
	leftLabel,
	rightLabel,
	labelSide = 'left',
	labelOffsetX = 0,
	rightLabelOffsetX = 0,
	labelOffsetY = -45,
	labelGap = 30,
	labelWidth = 140,
	labelFontSize = 14,
	labelLineHeight = 16,
	labelColor = '#000',
	labelAlign,
}: TriangleBlockProps) {
	const radius = size / 2
	const centerX = radius
	const centerY = radius
	const isLabelLeft = labelSide === 'left'
	const labelX = isLabelLeft
		? -labelGap - labelWidth + labelOffsetX
		: size + labelGap + labelOffsetX
	const effectiveLabelAlign = labelAlign ?? (isLabelLeft ? 'right' : 'left')
	
	const leftLabelX = -labelGap - labelWidth + labelOffsetX
	const rightLabelX = size + labelGap + rightLabelOffsetX
	
	const renderLabel = (labelText: string, xPos: number, align: 'left' | 'center' | 'right') => {
		if (labelText.includes('<span')) {
			return (
				<Html>
					<div
						style={{
							position: 'absolute',
							left: xPos,
							top: centerY - labelFontSize / 2 + labelOffsetY,
							width: labelWidth,
							fontSize: labelFontSize,
							lineHeight: labelLineHeight / labelFontSize,
							color: labelColor,
							textAlign: align === 'center' ? 'center' : align === 'right' ? 'right' : 'left',
							whiteSpace: 'pre-line',
							userSelect: 'none',
						}}
						dangerouslySetInnerHTML={{ __html: labelText.replace(/\n/g, '<br />') }}
					/>
				</Html>
			)
		} else {
			return (
				<Text
					text={labelText}
					x={xPos}
					y={centerY - labelFontSize / 2 + labelOffsetY}
					width={labelWidth}
					align={align}
					fontSize={labelFontSize}
					lineHeight={labelLineHeight / labelFontSize}
					fill={labelColor}
				/>
			)
		}
	}

	return (
		<Group x={x} y={y}>
			<Group rotation={rotation} offsetX={radius} offsetY={radius}>
				<Circle
					x={centerX}
					y={centerY}
					radius={radius}
					stroke="#000"
					strokeWidth={3}
					fill="#d3d3d3"
				/>

				<Line
					points={[
						centerX - radius * 0.39, centerY - radius * 0.86, // Левая верхняя точка
						centerX + radius * 0.95, centerY, // Правая точка (вершина)
						centerX - radius * 0.39, centerY + radius * 0.87 // Левая нижняя точка
					]}
					closed={true}
					fill="white"
					stroke="black"
					strokeWidth={1}
				/>
			</Group>

			{leftLabel && rightLabel ? (
				<>
					{renderLabel(leftLabel, leftLabelX, 'right')}
					{renderLabel(rightLabel, rightLabelX, 'left')}
				</>
			) : label ? (
				renderLabel(label, labelX, effectiveLabelAlign)
			) : null}
		</Group>
	)
}
