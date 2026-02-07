import { Group, Text, Line } from 'react-konva'

interface TemperatureBlockProps {
  width: number
  height: number
  text: string
  subText?: string
  x?: number
  y?: number
  rotation?: number
  labelOffsetX?: number
  labelOffsetY?: number
  labelWidth?: number
  keepLabelUpright?: boolean
  subTextFontSize?: number
  subTextColor?: string
  subTextOffsetY?: number
}

export function TemperatureBlock({
  width,
  height,
  text,
  subText,
  x = 0,
  y = 0,
  rotation = 0,
  labelOffsetX = 0,
  labelOffsetY = 0,
  labelWidth = 70,
  keepLabelUpright = true,
  subTextFontSize = 11,
  subTextColor = '#4b5563',
  subTextOffsetY = 0,
}: TemperatureBlockProps) {
  const triangleSize = height * 0.8
  const lineLength = width * 0.3

  const shapeX = labelWidth + 3
  const centerY = height / 2

  return (
    <Group x={x} y={y}>
      <Text
        text={text}
        x={labelOffsetX}
        y={centerY - 8 + labelOffsetY}
        width={labelWidth}
        align="right"
        fontSize={16}
        fill="#000"
        fontFamily="Inter, Arial, sans-serif"
        rotation={keepLabelUpright ? 0 : rotation}
      />
      {subText ? (
        <Text
          text={subText}
          x={labelOffsetX}
          y={centerY + 8 + labelOffsetY + subTextOffsetY}
          width={labelWidth}
          align="right"
          fontSize={subTextFontSize}
          fill={subTextColor}
          fontFamily="Inter, Arial, sans-serif"
          rotation={keepLabelUpright ? 0 : rotation}
        />
      ) : null}

      <Group x={shapeX} y={centerY} rotation={rotation}>
        <Line
          points={[
            0 +5, -triangleSize / 2,
            triangleSize, 0,
            0 +5, triangleSize / 2,
          ]}
          closed
          fill="#000"
          stroke="#000"
          strokeWidth={1}
        />

        <Line
          points={[triangleSize, 0, triangleSize + lineLength, 0]}
          stroke="#000"
          strokeWidth={2}
        />
      </Group>
    </Group>
  )
}