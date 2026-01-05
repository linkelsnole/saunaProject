import { Line, Rect, Text } from 'react-konva';
import useHover from '../hooks/useHover';
import useElementClick from '../hooks/useElementClick';

const PRect = ({x, y, width, height, label, onDPClick }) => {

	const [isHovered, hoverHandlers] = useHover();
	const clickHandlers = useElementClick(onDPClick, label);

	return (
   <>
			<Rect 
				x={x} 
				y={y} 
				width={width} 
				height={height} 
				stroke="#23406e" 
				strokeWidth={3} 
				fill={isHovered ? "#7AD88F" : "#fff"}
				{...hoverHandlers}
				{...clickHandlers}
				cursor="pointer"
			/>
      <Text 
				x={x + 14} 
				y={y + 8} 
				text={label} 
				fontSize={16} 
				fill="black" 
				{...hoverHandlers}
				{...clickHandlers}
				cursor="pointer"
			/>
		</>
	)
}

export default PRect;

