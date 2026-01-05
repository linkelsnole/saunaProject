import { Circle, Line, Text, Rect } from 'react-konva';
const Point = ({ x, y, id }) => {
  return (
    <>
      <Circle x={x} y={y} radius={7} stroke="#23406e" strokeWidth={1} fill="#507bb4" />
      {id && <Text x={x-15} y={y+15} text={id} fontSize={16} fill="#23406e" />}
    </>
  );
};
export default Point;