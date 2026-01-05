import { Group, Rect, Text, Line } from 'react-konva'

interface PressureBlockProps {
	width: number
	height: number
	text: string
	x?: number
	y?: number
	rotation?: number
	labelWidth?: number
	labelOffsetX?: number
	labelOffsetY?: number
	labelFontSize?: number
	labelColor?: string
	labelAlign?: 'left' | 'center' | 'right'
}

export function PressureBlock({
	width,
	height,
	text,
	x = 0,
	y = 0,
	rotation = 0,
	labelWidth = 60,
	labelOffsetX = 0,
	labelOffsetY = 0,
	labelFontSize = 16,
	labelColor = '#000',
	labelAlign = 'right',
}: PressureBlockProps) {
	const rectX = labelWidth + 8
	const labelY = height / 2 - labelFontSize / 2 + labelOffsetY
	const shapeOffsetX = -width / 2
	const shapeOffsetY = -height / 2
	const shapeX = rectX + width / 2
	const shapeY = height / 2

	return (
		<Group x={x} y={y}>
			<Text
				text={text}
				x={labelOffsetX}
				y={labelY}
				width={labelWidth}
				align={labelAlign}
				fontSize={labelFontSize}
				fontStyle="normal"
				fill={labelColor}
			/>

			<Group x={shapeX} y={shapeY} rotation={rotation}>
				<Rect
					x={shapeOffsetX}
					y={shapeOffsetY}
					width={width}
					height={height}
					stroke="#000"
					strokeWidth={2}
					fill="#fff"
				/>

				<Line
					points={[
						width * 0.025 + shapeOffsetX, height * 0.01 + shapeOffsetY,
						width * 0.25 + shapeOffsetX, height * 0.95 + shapeOffsetY,
						width * 0.5 + shapeOffsetX, height * 0.05 + shapeOffsetY,
						width * 0.75 + shapeOffsetX, height * 0.95 + shapeOffsetY,
						width * 0.975 + shapeOffsetX, height * 0.01 + shapeOffsetY
					]}
					stroke="#000"
					strokeWidth={3}
					lineCap="round"
					lineJoin="round"
				/>
			</Group>
		</Group>
	)
}
