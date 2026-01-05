import React from "react";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { BellIcon } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator
} from "@/shared/ui/dropdown-menu";

const NotificationButton: React.FC = () => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="icon" className="relative h-10 w-10 border-border/40">
          <BellIcon className=""/>
          <Badge className="absolute -top-1 -right-1 h-5 w-5 rounded-full p-0 flex items-center justify-center bg-red-500 text-white">
            <span className="text-xs">3</span>
          </Badge>
          <span className="sr-only">Уведомления</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <div className="flex items-center justify-between p-2">
          <h4 className="font-semibold text-base">Уведомления</h4>
          <Button variant="ghost" size="sm" className="text-sm">
            Отметить все
          </Button>
        </div>
        <DropdownMenuSeparator />
        <div className="max-h-[300px] overflow-y-auto">
          <NotificationItem 
            title="Высокая температура" 
            description="Зафиксирована высокая температура в датчике"
            time="2 минуты назад"
            isNew
          />
          <NotificationItem 
            title="Превышен уровень CO₂" 
            description="Уровень CO₂ превысил 800 ppm в помещении"
            time="5 минут назад"
            isNew
          />
          <NotificationItem 
            title="Обновление системы" 
            description="Доступно новое обновление v2.3.4"
            time="1 час назад"
            isNew
          />
          <NotificationItem 
            title="Соединение восстановлено" 
            description="Соединение с сервером успешно восстановлено"
            time="3 часа назад"
          />
          <NotificationItem 
            title="Техническое обслуживание" 
            description="Плановое обслуживание 5 мая, 10:00-14:00"
            time="Вчера"
          />
        </div>
        <DropdownMenuSeparator />
        <DropdownMenuItem className="cursor-pointer justify-center font-medium text-base">
          Все уведомления
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

interface NotificationItemProps {
  title: string;
  description: string;
  time: string;
  isNew?: boolean;
}

const NotificationItem: React.FC<NotificationItemProps> = ({ 
  title, 
  description, 
  time, 
  isNew = false 
}) => {
  return (
    <div className={`p-3 hover:bg-muted cursor-pointer ${isNew ? 'bg-primary/5' : ''}`}>
      <div className="flex justify-between items-start">
        <h5 className="font-medium text-sm">{title}</h5>
        <span className="text-xs text-muted-foreground">{time}</span>
      </div>
      <p className="text-sm text-muted-foreground mt-1 line-clamp-2">{description}</p>
      {isNew && (
        <Badge className="mt-2 bg-primary/20 text-primary border-0 hover:bg-primary/30">
          Новое
        </Badge>
      )}
    </div>
  );
};

export default NotificationButton;
