import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ManualSettingsWidget } from './ManualSettingsWidget';
import { TempSettingsWidget } from './TempSettingsWidget';
import { RecuperatorWidget } from './RecuperatorWidget';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs';

export const SettingsWidget = () => {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState('volume');

  return (
    <div className="h-full flex flex-col overflow-hidden">
      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 min-h-0 h-full flex flex-col overflow-hidden">
        <TabsList className="grid w-full grid-cols-3 mb-4 shrink-0">
          <TabsTrigger value="volume">{t('app.tabs.volume')}</TabsTrigger>
          <TabsTrigger value="temperature">{t('app.tabs.temperature')}</TabsTrigger>
          <TabsTrigger value="recuperator">{t('app.tabs.recuperator')}</TabsTrigger>
        </TabsList>
        
        <TabsContent value="volume" className="flex-1 overflow-y-auto">
          <ManualSettingsWidget />
        </TabsContent>
        
        <TabsContent value="temperature" className="flex-1 overflow-y-auto">
          <TempSettingsWidget />
        </TabsContent>
        
        <TabsContent value="recuperator" className="flex-1 overflow-y-auto">
          <RecuperatorWidget />
        </TabsContent>
      </Tabs>
    </div>
  );
};

