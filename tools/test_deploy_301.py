import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('deploy301', Path(__file__).resolve().parents[1] / 'deploy-301.py')
client = importlib.util.module_from_spec(spec)
spec.loader.exec_module(client)

class Deploy301Tests(unittest.TestCase):
    def environment(self, *args):
        with patch.object(client.sys, 'argv', ['deploy-301.py', '--build', *args]), \
             patch.object(client.subprocess, 'call', return_value=0) as call:
            self.assertEqual(client.main(), 0)
            return call.call_args.kwargs['env']

    def test_omitted_override_uses_embedded_default(self):
        with patch.dict(client.os.environ, {'MODBUS_LOG_NAME': 'OldValue'}):
            self.assertNotIn('MODBUS_LOG_NAME', self.environment())

    def test_home_override_and_port(self):
        env = self.environment('--log-name', 'HomePLC', '--data-port', '4569')
        self.assertEqual(env['MODBUS_LOG_NAME'], 'HomePLC')
        self.assertEqual(env['MODBUS_API_PORT'], '4569')

    def test_explicit_empty_name_is_preserved(self):
        self.assertEqual(self.environment('--log-name', '')['MODBUS_LOG_NAME'], '')

    def test_invalid_name_never_starts_deploy(self):
        with patch.object(client.sys, 'argv', ['deploy-301.py', '--log-name', 'bad name']), \
             patch.object(client.subprocess, 'call') as call:
            with self.assertRaises(SystemExit):
                client.main()
            call.assert_not_called()

if __name__ == '__main__':
    unittest.main()
