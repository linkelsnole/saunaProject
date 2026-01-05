import { Group, Rect, Circle, Line } from 'react-konva'

interface DiagonalBlockProps {
	size: number
	x?: number
	y?: number
	isOpen?: boolean | null  // true - открыто (зеленый), false/null - закрыто (серый)
}

export function DiagonalBlock({ size, x = 0, y = 0, isOpen = null }: DiagonalBlockProps) {
	const circleSize = size * 0.3 // Размер круга
	const circleColor = isOpen === true ? '#22c55e' : '#9ca3af' // зеленый если открыто, серый если закрыто

	return (
		<Group x={x} y={y}>
			{/* Квадрат с серым фоном */}
			<Rect
				x={0}
				y={0}
				width={size}
				height={size}
				stroke="#000"
				strokeWidth={2}
				fill="#d3d3d3"
			/>

			{/* Диагональная линия от нижнего левого до верхнего правого угла */}
			<Line
				points={[0, size, size, 0]}
				stroke="#000"
				strokeWidth={3}
			/>

			{/* Круг в центре - зеленый если открыто, серый если закрыто */}
			<Circle
				x={size / 2}
				y={size / 2}
				radius={circleSize / 2}
				fill={circleColor}
				stroke="#000"
				strokeWidth={2}
			/>

		</Group>
	)
}

