package com.example.modbus_api.modbus;

import com.example.modbus_api.modbus.model.*;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;
import java.util.concurrent.TimeUnit;

public class ModbusAPIClient {
    public final static int HTTPTimeOut=60;                     // Тайм-аут клиента
    public final static int HTTPOk = 200;
    public final static int HTTPAuthorization = 401;
    public final static int HTTPNotFound = 404;
    public final static int HTTPRequestError = 400;
    public final static int HTTPException = 500;
    public final static int HTTPServiceUnavailable = 503;  // Сервис недоступен
    //-------------------------------------------------------------------------------------------------------
    public abstract static class APICall<T> {
        public abstract Call<T> apiFun();
        public T call() throws UniException{
            try {
                Response<T> res = apiFun().execute();
                if (!res.isSuccessful()){
                    if (res.code()== HTTPAuthorization){
                        UniException.io("Сеанс закрыт " + Utils.httpError(res));
                        }
                    UniException.io("Ошибка " + res.message()+" ("+res.code()+") "+res.errorBody().string());
                    }
                return res.body();
                } catch (Exception ex) {
                    throw UniException.io(ex);
                    }
            }
        }
    //-------------------------------------------------------------------------------------------------------
    public interface RestAPIModbus {
        /** Активность клиента - в ответе - количество неподтвержденных уведомлений */
        @GET("/api/keepalive")
        Call<JInt> keepalive(@Header("SessionToken") String token);
        /** Авторизация по номеру телефона (логину) и паролю */
        @GET("/api/user/login/phone")
        Call<User> login(@Query("phone") String phone, @Query("pass") String pass);
        /** Прочитать регистр ПЛК по номеру */
        @GET("/api/ess2/register/value/read")
        Call<JInt> readESS2RegisterValue(@Header("SessionToken") String token,@Query("deviceName") String deviceName,@Query("unit") int unit,@Query("regnum") int regnum, @Query("equipControl") boolean equipControl);
        /** Записать регистр ПЛК по номеру */
        @POST("/api/ess2/register/value/write")
        Call<JEmpty> writeESS2RegisterValue(@Header("SessionToken") String token, @Query("deviceName") String deviceName, @Query("unit") int unit, @Query("regnum") int regnum, @Query("value") int value, @Query("equipControl") boolean equipControl);
        /** Прочитать регистры device по заданному списку */
        @POST("/api/ess2/registers/list/read")
        Call<IntegerList> readESS2RegistersValues(@Header("SessionToken") String token, @Query("deviceName") String deviceName,@Body UnitRegisterList list);
        /** Прочитать регистр ПЛК по номеру */
        @GET("/api/ess2/register/logunit/read")
        Call<JInt> readESS2RegisterByLogUnit(@Header("SessionToken") String token,@Query("logName") String deviceName,@Query("regnum") int regnum);
        /** Записать регистр ПЛК по номеру */
        @POST("/api/ess2/register/logunit/write")
        Call<JEmpty> writeESS2RegisterByLogUnit(@Header("SessionToken") String token,@Query("logName") String deviceName,@Query("regnum") int regnum,@Query("value") int value);
        /** Прочитать регистры device по заданному списку */
        @POST("/api/ess2/register/list/logunit/read")
        Call<IntegerList> readESS2RegisterListByLogUnit(@Header("SessionToken") String token, @Query("logName") String deviceName,@Body UnitRegisterList list);
        }
    public static Object startClient(String ip, String port, Class restAPI) throws UniException {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(HTTPTimeOut, TimeUnit.SECONDS)
                .connectTimeout(HTTPTimeOut, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://"+ip+":"+port)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return  retrofit.create(restAPI);
        }

    public static void main(String ss[]) throws UniException {
        final String ip = "172.16.6.119";
        //final String ip = "localhost";
        final String port = "4567";
        final RestAPIModbus api3 = (RestAPIModbus) startClient(ip,port,RestAPIModbus.class);    // Короткое API для регистров Modbus
        final String sensorDevice = "Sensors";
        final String controlDevice = "AirPLC";
        final String logUnit2 = "AirPLC";
        final String logUnit = "T1";
        User user = new APICall<User>() {
            @Override
            public Call<User> apiFun() {
                return api3.login("9131111111","1234");
                }
            }.call();
        String token = user.getSessionToken();
        System.out.println("token="+token);
        //----------------- Работа с логическими устройствами -----------------------------------------------------
        UnitRegisterList list = new UnitRegisterList();
        list.setUnitIdx(2);
        for(int reg=0x020; reg<=0x025;reg++)
            list.add(reg);
        System.out.println();

        // --- T1 ---
        // Температура
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0100);
            }
        }.call();
        System.out.println("AirPLC T1: температура 0x0100 = "+val.getValue()/10.0 + "°C");

        // Влажность
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0101);
            }
        }.call();
        System.out.println("AirPLC T1: влажность 0x0101 = "+val.getValue()/10.0 + "%");

        // Вид аварии
        // 0 - Normal (Норма)
        // 1 - Warning (Предупреждение)
        // 2 - Alarm (Неисправность)
        // 3 - Failure (Авария)
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0102);
            }
        }.call();
        System.out.println("AirPLC T1: вид аварии (состояние) 0x0102 = "+val.getValue());

        // Продолжительность аварии
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0103);
            }
        }.call();
        System.out.println("AirPLC T1: продолжительность состояния 0x0103 = "+val.getValue() + " c");
        System.out.println();

        // --- T2 ---
        // Температура
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0110);
            }
        }.call();
        System.out.println("AirPLC T2: температура 0x0110 = "+val.getValue()/10.0 + "°C");

        // Влажность
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0111);
            }
        }.call();
        System.out.println("AirPLC T2: влажность 0x0111 = "+val.getValue()/10.0 + "%");

        // Вид аварии
        // 0 - Normal (Норма)
        // 1 - Warning (Предупреждение)
        // 2 - Alarm (Неисправность)
        // 3 - Failure (Авария)
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0112);
            }
        }.call();
        System.out.println("AirPLC T2: вид аварии (состояние) 0x0112 = "+val.getValue());

        // Продолжительность аварии
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0113);
            }
        }.call();
        System.out.println("AirPLC T2: продолжительность состояния 0x0113 = "+val.getValue() + " c");
        System.out.println();

        // --- T3 ---
        // Температура
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0120);
            }
        }.call();
        System.out.println("AirPLC T3: температура 0x0120 = "+val.getValue()/10.0 + "°C");

        // Влажность
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0121);
            }
        }.call();
        System.out.println("AirPLC T3: влажность 0x0121 = "+val.getValue()/10.0 + "%");

        // Вид аварии
        // 0 - Normal (Норма)
        // 1 - Warning (Предупреждение)
        // 2 - Alarm (Неисправность)
        // 3 - Failure (Авария)
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0122);
            }
        }.call();
        System.out.println("AirPLC T3: вид аварии (состояние) 0x0122 = "+val.getValue());

        // Продолжительность аварии
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0123);
            }
        }.call();
        System.out.println("AirPLC T3: продолжительность состояния 0x0123 = "+val.getValue() + " c");
        System.out.println();

        // --- T4 ---
        // Температура
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0130);
            }
        }.call();
        System.out.println("AirPLC T4: температура 0x0130 = "+val.getValue()/10.0 + "°C");

        // Влажность
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0131);
            }
        }.call();
        System.out.println("AirPLC T4: влажность 0x0131 = "+val.getValue()/10.0 + "%");

        // Вид аварии
        // 0 - Normal (Норма)
        // 1 - Warning (Предупреждение)
        // 2 - Alarm (Неисправность)
        // 3 - Failure (Авария)
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0132);
            }
        }.call();
        System.out.println("AirPLC T4: вид аварии (состояние) 0x0132 = "+val.getValue());

        // Продолжительность аварии
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0133);
            }
        }.call();
        System.out.println("AirPLC T4: продолжительность состояния 0x0133 = "+val.getValue() + " c");
        System.out.println();

        // --- Состояние агрегата ---
        // 0 - Off (Выкл.)
        // 1 - Fail (Авария фатальная)
        // 2 - Alarm (Авария с восстановлением)
        // 3 - Recovery (Запуск до текущей точки)
        // 4 - Start (Начальный запуск)
        // 5 - Drift (Дрейф до точки регулирования)
        // 6 - Working (Вентилирование)
        // 7 - RunOut (Остановка агрегата, выбег)
        // 8 - PSBlow (Продувка датчика давления)
        // 9 - HitCooling (Охладжение калорифера)
        // 10 - ExRunOut (Остаточная работа теплообменника)
        // 11 - ExBlow (Чистительный продув теплообменника)
        // 12 - Hood (Вытяжка)
        // 13 - ExCalibration (Калибровка вращения теплообменника)
        // 14 - ExDefrosting (Размораживание теплообменника)
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0010);
            }
        }.call();
        System.out.println("AirPLC: состояние агрегата 0x0010= "+val.getValue());

        // --- Состояние агрегата 2 ---
        // 0 - More
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0011);
            }
        }.call();
        System.out.println("AirPLC: состояние агрегата 2 0x0011= "+val.getValue());
        System.out.println();



        // --- Калибровка теплообменника ---
        // 0 - ExCalibMode1 (Начальная)
        // 1 - ExCalibMode2 (Смена режима)
        // 2 - ExCalibMode3 (Периодическая)
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0031);
            }
        }.call();
        System.out.println("AirPLC: Калибровка теплообменника 0x0031= "+val.getValue());
        System.out.println();

        // --- Уставки ---
        // Нижняя граница CO2 Eco
        // Запись
        JEmpty vv = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0020,88);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0020);
            }
        }.call();
        System.out.println("AirPLC CO2: Нижняя граница CO2 Eco 0x0020= "+val.getValue() + " ppm");

        // Верхняя граница CO2 Eco
        // Запись
        JEmpty vv2 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0021,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0021);
            }
        }.call();
        System.out.println("AirPLC CO2: Верхняя граница CO2 Eco 0x0021= "+val.getValue()  + " ppm");

        // Нижняя граница CO2 Norm
        // Запись
        JEmpty vv3 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0022,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0022);
            }
        }.call();
        System.out.println("AirPLC CO2: Нижняя граница CO2 Norm 0x0022= "+val.getValue() + " ppm");

        // Верхняя граница CO2 Norm
        // Запись
        JEmpty vv4 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0023,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0023);
            }
        }.call();
        System.out.println("AirPLC CO2: Верхняя граница CO2 Norm 0x0023= "+val.getValue() + " ppm");

        // Нижняя граница CO2 Best
        // Запись
        JEmpty vv5 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0024,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0024);
            }
        }.call();
        System.out.println("AirPLC CO2: Нижняя граница CO2 Best 0x0024= "+val.getValue() + " ppm");

        // Верхняя граница CO2 Best
        // Запись
        JEmpty vv6 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0025,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0025);
            }
        }.call();
        System.out.println("AirPLC CO2: Верхняя граница CO2 Best 0x0025= "+val.getValue() + " ppm");
        System.out.println();

        // Нижняя граница объём Eco
        // Запись
        JEmpty vv7 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0026,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0026);
            }
        }.call();
        System.out.println("AirPLC Air: Нижняя граница CO2 Eco 0x0026= "+val.getValue() + " м3/час");

        // Верхняя граница объём Eco
        // Запись
        JEmpty vv8 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0027,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0027);
            }
        }.call();
        System.out.println("AirPLC Air: Верхняя граница CO2 Eco 0x0027= "+val.getValue()  + " м3/час");

        // Нижняя граница объём Norm
        // Запись
        JEmpty vv9 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0028,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0028);
            }
        }.call();
        System.out.println("AirPLC Air: Нижняя граница CO2 Norm 0x0028= "+val.getValue() + " м3/час");

        // Верхняя граница объём Norm
        // Запись
        JEmpty vv10 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0029,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0029);
            }
        }.call();
        System.out.println("AirPLC Air: Верхняя граница CO2 Norm 0x0029= "+val.getValue() + " м3/час");

        // Нижняя граница объем Best
        // Запись
        JEmpty vv11 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x002A,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x002A);
            }
        }.call();
        System.out.println("AirPLC Air: Нижняя граница CO2 Best 0x002A= "+val.getValue() + " м3/час");

        // Верхняя граница объём Best
        // Запись
        JEmpty vv12 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x002B,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x002B);
            }
        }.call();
        System.out.println("AirPLC Air: Верхняя граница CO2 Best 0x002B= "+val.getValue() + " м3/час");
        System.out.println();

        // --- Основной регистр комманд ---
        // 1 - пуск агрегата
        // 2 - останов агрегата
        // 3 - жесткий сброс
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0012);
            }
        }.call();
        System.out.println("AirPLC: Основной регистр комманд 0x0012= "+val.getValue());

        /*
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0001);
                }
            }.call();
        System.out.println("T1: температура 0x0001 = "+val.getValue()/10.0);


        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0100);
                }
            }.call();
        System.out.println("AirPLC T1: температура 0x0100 = "+val.getValue()/10.0);
        JEmpty vv = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x0005,66);
                }
            }.call();
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0005);
                }
            }.call();
        System.out.println("T1: уставка 0x0005 = "+val.getValue());
        IntegerList val2 = new APICall<IntegerList>() {
            @Override
            public Call<IntegerList> apiFun() {
                return api3.readESS2RegisterListByLogUnit(token,logUnit2,list);
            }
        }.call();
        System.out.print("МУ: уставки 0x0020-0x0025 = ");
        for(Integer vv2 : val2)
            System.out.print(vv2+" ");
        System.out.println();
        */

        //------------------------------------------------------------------------------------
        // НОВЫЕ ДАННЫЕ 
        //------------------------------------------------------------------------------------
        
        // ДОПОЛНИТЕЛЬНЫЕ ЛОГИЧЕСКИЕ УСТРОЙСТВА
        final String logUnit_M1 = "M1";          // Вентилятор 1
        final String logUnit_M2 = "M2";          // Вентилятор 2
        final String logUnit_TH1 = "TH1";        // Нагреватель
        final String logUnit_CD = "CD";          // Заслонки
        final String logUnit_EM1 = "EM1";        // Счётчик энергии
        final String logUnit_T5 = "T5";          // CO2 датчик
        final String logUnit_MRC1 = "MRC1";      // Рекуператор
				final String logUnit_MV12 = "AirPLC"; // Объем воздухом 


        // --- ВЕНТИЛЯТОРЫ ---
				val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x40000);
					}
			}.call();
			System.out.println("ECMotor M1: Версия оборудования 0x40000 = "+val.getValue());

			val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x40000);
					}
			}.call();
			System.out.println("ECMotor M2: Версия оборудования 0x40000 = "+val.getValue());

			val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x40001);
					}
			}.call();
			System.out.println("ECMotor M1: Версия прошивки 0x40001 = "+val.getValue());

			val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x40001);
					}
			}.call();
			System.out.println("ECMotor M2: Версия прошивки 0x40001 = "+val.getValue());
        // // Версия
        // val = new APICall<JInt>() {
        //     @Override
        //     public Call<JInt> apiFun() {
        //         return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x0000);
        //     }
        // }.call();
        // System.out.println("ECMotor M1: Версия оборудования 0x0000 = "+val.getValue());

        // val = new APICall<JInt>() {
        //     @Override
        //     public Call<JInt> apiFun() {
        //         return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x0000);
        //     }
        // }.call();
        // System.out.println("ECMotor M2: Версия оборудования 0x0000 = "+val.getValue());

        // val = new APICall<JInt>() {
        //     @Override
        //     public Call<JInt> apiFun() {
        //         return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x0001);
        //     }
        // }.call();
        // System.out.println("ECMotor M1: Версия прошивки 0x0001 = "+val.getValue());

        // val = new APICall<JInt>() {
        //     @Override
        //     public Call<JInt> apiFun() {
        //         return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x0001);
        //     }
        // }.call();
        // System.out.println("ECMotor M2: Версия прошивки 0x0001 = "+val.getValue());

        // ТЕКУЩИЕ ОБОРОТЫ ВЕНТИЛЯТОРОВ
        // Запись
        JEmpty vv1 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit_M1,0x0004,400);
            }
        }.call();

        JEmpty vv2_M2 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit_M2,0x0004,400);
            }
        }.call();


				// Чтение
        val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x40004);
					}
			}.call();
			System.out.println("ECMotor M1: Скорость 0x40004 = "+val.getValue() + " Об/мин");

			val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x40004);
					}
			}.call();
			System.out.println("ECMotor M2: Скорость 0x40004 = "+val.getValue() + " Об/мин");

			// Температура контроллера
			val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x40005);
					}
			}.call();
			System.out.println("ECMotor M1: Температура контроллера 0x40005 = "+val.getValue()/100 + "°C");

			val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x40005);
					}
			}.call();
			System.out.println("ECMotor M2: Температура контроллера 0x40005 = "+val.getValue()/100 + "°C");

			// Мощность
			val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x40008);
					}
			}.call();
			System.out.println("ECMotor M1: Мощность 0x40008 = "+val.getValue() + " Вт");

			val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x40008);
					}
			}.call();
			System.out.println("ECMotor M2: Мощность 0x40008 = "+val.getValue() + " Вт");

    //     // Чтение
    //     val = new APICall<JInt>() {
    //         @Override
    //         public Call<JInt> apiFun() {
    //             return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x0004);
    //         }
    //     }.call();
    //     System.out.println("ECMotor M1: Скорость 0x0004 = "+val.getValue() + " Об/мин");

    //     val = new APICall<JInt>() {
    //         @Override
    //         public Call<JInt> apiFun() {
    //             return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x0004);
    //         }
    //     }.call();
    //     System.out.println("ECMotor M2: Скорость 0x0004 = "+val.getValue() + " Об/мин");

    //     // Температура контроллера
    //     val = new APICall<JInt>() {
    //         @Override
    //         public Call<JInt> apiFun() {
    //             return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x0005);
    //         }
    //     }.call();
    //     System.out.println("ECMotor M1: Температура контроллера 0x0005 = "+val.getValue() + "°C");

    //     val = new APICall<JInt>() {
    //         @Override
    //         public Call<JInt> apiFun() {
    //             return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x0005);
    //         }
    //     }.call();
    //     System.out.println("ECMotor M2: Температура контроллера 0x0005 = "+val.getValue() + "°C");

    //     // Мощность
    //     val = new APICall<JInt>() {
    //         @Override
    //         public Call<JInt> apiFun() {
    //             return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x0008);
    //         }
    //     }.call();
    //     System.out.println("ECMotor M1: Мощность 0x0008 = "+val.getValue() + " Вт");

    //     val = new APICall<JInt>() {
    //         @Override
    //         public Call<JInt> apiFun() {
    //             return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x0008);
    //         }
    //     }.call();
    //     System.out.println("ECMotor M2: Мощность 0x0008 = "+val.getValue() + " Вт");
		
		// % МАКСИМАЛЬНО-ДОПУСТИМАЯ СКОРОСТЬ ВРАЩЕНИЯ
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x30002);
            }
        }.call();
        System.out.println("ECMotor M1: Макс Скорость 0x30002 = "+val.getValue() + " Об/мин");
				

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x30002);
            }
        }.call();
        System.out.println("ECMotor M2: % Макс Скорость 0x30002 = "+val.getValue() + " Об/мин");
		

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x3001B);
            }
        }.call();
        System.out.println("ECMotor M1: % Скорости 0x3001B = "+val.getValue() + " %");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x3001B);
            }
        }.call();
        System.out.println("ECMotor M2: % Скорости 0x3001B = "+val.getValue() + " %");


				// Объем расход воздуха (м3/ч)
				val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_MV12,0x0047);
					}
			}.call();
			System.out.println("ECMotor M1: Объём/расход воздуха 0x0047 = "+val.getValue() + " м3/ч");

				val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_MV12,0x0048);
					}
			}.call();
			System.out.println("ECMotor M2: Объём/расход воздуха 0x0048 = "+val.getValue() + " м3/ч");
		
        // Светодиод
        // 0 - Многократно---
        // 1 - Напряжение<>
        // 2 - Неисправность
        // 3 - Преобр. частоты
        // 4 - Защ. от перегрева
        // 5 - Защ. по току
        // 6 - Ожидание
        // 7 - Нормально
        // -1 - Пожар
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_M1,0x0013);
            }
        }.call();
        System.out.println("ECMotor M1: Код ошибки светодиода 0x0013 = "+val.getValue());

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_M2,0x0013);
            }
        }.call();
        System.out.println("ECMotor M2: Код ошибки светодиода 0x0013 = "+val.getValue());
        System.out.println();

        // --- НАГРЕВАТЕЛЬ ---
        // ПРОЦЕНТ НАГРЕВА
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_TH1,0x0003);
            }
        }.call();
        System.out.println("TCN6: Процент нагрева 0x0003 = "+val.getValue() + "%");

        // Светодиод
        // 0 - Primary Control Output 1
        // 2 - 2
        // 3 - Alarm 1
        // 4 - Alarm 2
        // 5 - Output 2
        // 6 - Manual/Automatic
        // 8 - Timer Start Flag
        // 9 - Timer End Flag
        // 10 - Timer Control Primary Output Off Flag
        // 11 - Timer End Alarm Output Flag
        // 12 - Alarm Output Timer Working Flag
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_TH1,0x0001);
            }
        }.call();
        System.out.println("TCN6: Код ошибки светодиода 0x0001 = "+val.getValue());
        System.out.println();

        // --- ЗАСЛОНКИ (ЗАДВИЖКИ) ---
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_CD,0x0000);
            }
        }.call();
        System.out.println("CD EXV1: Процент открытия заслонки 0x0000 = "+val.getValue() + "%");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_CD,0x0001);
            }
        }.call();
        System.out.println("CD EXV2: Процент открытия заслонки 0x0001 = "+val.getValue() + "%");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_CD,0x0002);
            }
        }.call();
        System.out.println("CD EXV3: Процент открытия заслонки 0x0002 = "+val.getValue() + "%");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_CD,0x0003);
            }
        }.call();
        System.out.println("CD EXV4: Процент открытия заслонки 0x0003 = "+val.getValue() + "%");
        System.out.println();

        // --- СЧЕТЧИК ЭНЕРГИИ ---
        // Напряжение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x0000);
            }
        }.call();
        System.out.println("EM1: Напряжение 0x0000 = "+val.getValue() + " В");

        //Ток
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x0003);
            }
        }.call();
        System.out.println("EM1: Ток 0x0003 = "+val.getValue() + " А");

        // Активная мощность
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x0008);
            }
        }.call();
        System.out.println("EM1: Активная мощность 0x0008 = "+val.getValue() + " Вт");

        // Косинус фи
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x0014);
            }
        }.call();
        System.out.println("EM1: Косинус фи 0x0014 = "+val.getValue());

        // Частота
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x001A);
            }
        }.call();
        System.out.println("EM1: Частота 0x001A = "+val.getValue() + "Гц");

        // ОБЩАЯ АКТИВНАЯ ЭНЕРГИЯ (потребление для вывода в счётчик)
				// val = new APICall<JInt>() {
        //     @Override
        //     public Call<JInt> apiFun() {
        //         return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x001D);
        //     }
        // }.call();
        // System.out.println("EM1: Общая активная энергия 0x001D = "+val.getValue() + " КВт/ч");


        // Положительная активная энергия
        // val = new APICall<JInt>() {
        //     @Override
        //     public Call<JInt> apiFun() {
        //         return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x0027);
        //     }
        // }.call();
        // System.out.println("EM1: Положительная активная энергия 0x0027 = "+val.getValue() + " КВт/ч");

				// Общая активная энергия
				JInt val2;
				val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x4001D);
					}
			}.call();
			System.out.println("EM1: Общая активная энергия 0x4001D = "+val.getValue() + " КВТ•ч");

				val2 = new APICall<JInt>() {
						@Override
						public Call<JInt> apiFun() {
								return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x4001D+1);
						}
				}.call();
				System.out.println("EM1: Общая активная энергия 0x4001D+1 = "+val2.getValue() + " КВТ•ч");

				int v1 = val.getValue(); // чтение интовых значений
				int v2 = val2.getValue();
				v1 = v1 << 16; // побитовый сдвиг
				v1 = v1 | v2; // склейка двух регистров
				System.out.println("EM1: Общая активная энергия = "+v1 + " КВТ•ч");

				System.out.println();
				// Положительная активная энергия
				val = new APICall<JInt>() {
						@Override
						public Call<JInt> apiFun() {
								return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x40027);
						}
				}.call();
				System.out.println("EM1: Положительная активная энергия 0x40027 = "+val.getValue() + " КВТ•ч");


				val2 = new APICall<JInt>() {
						@Override
						public Call<JInt> apiFun() {
								return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x40027+1);
						}
				}.call();
				System.out.println("EM1: Положительная активная энергия 0x40027+1 = "+val2.getValue() + " КВТ•ч");

				int v11 = val.getValue(); // чтение интовых значений
				int v22 = val2.getValue();
				v11 = v11 << 16; // побитовый сдвиг
				v11 = v11 | v22; // склейка двух регистров
				System.out.println("EM1: Общая активная энергия = "+v11 + " КВТ•ч");

        // Обратная активная энергия
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_EM1,0x0031);
            }
        }.call();
        System.out.println("EM1: Обратная активная энергия 0x0031 = "+val.getValue() + " КВт/ч");
        System.out.println();

        // --- CO2 ДАТЧИК ---
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_T5,0x0000);
            }
        }.call();
        System.out.println("CO2Sensor: Температура 0x0000 = "+val.getValue()/10.0 + "°C");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_T5,0x0001);
            }
        }.call();
        System.out.println("CO2Sensor: Относительная влажность 0x0001 = "+val.getValue()/10.0 + "%");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_T5,0x0002);
            }
        }.call();
        System.out.println("CO2Sensor: Концентрация CO2 0x0002 = "+val.getValue() + " ppm");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_T5,0x0003);
            }
        }.call();
        System.out.println("CO2Sensor: Концентрация PM2.5 0x0003 = "+val.getValue() + " мг/m3");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_T5,0x0004);
            }
        }.call();
        System.out.println("CO2Sensor: Концентрация PM10 0x0004 = "+val.getValue() + " мг/m3");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_T5,0x0005);
            }
        }.call();
        System.out.println("CO2Sensor: Концентрация летучей органики 0x0005 = "+val.getValue() + " ppm");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_T5,0x0006);
            }
        }.call();
        System.out.println("CO2Sensor: CO2 эквивалент VOC 0x0006 = "+val.getValue() + " ppm");

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_T5,0x0007);
            }
        }.call();
        System.out.println("CO2Sensor: Концентрация формальдегида 0x0007 = "+val.getValue() + " ppm");
        System.out.println();

        // --- РЕКУПЕРАТОР ---
        // Время фильтрации
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0011);
            }
        }.call();
        System.out.println("CL57R: Время фильтрации DI 0x0011 = "+val.getValue() + " мс");

        // Номер привода
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0020);
            }
        }.call();
        System.out.println("CL57R: Номер привода 0x0020 = "+val.getValue());

        // Сегментация
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0023);
            }
        }.call();
        System.out.println("CL57R: Сегментация 0x0023 = "+val.getValue());

        // Стартовая скорость
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0030);
            }
        }.call();
        System.out.println("CL57R: Стартовая скорость 0x0030 = "+val.getValue() + " об/мин");

        // Время ускорения
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0031);
            }
        }.call();
        System.out.println("CL57R: Время ускорения 0x0031 = "+val.getValue() + " мс");

        // Время замедления
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0032);
            }
        }.call();
        System.out.println("CL57R: Время замедления 0x0032 = "+val.getValue() + " мс");

        // МАКСИМАЛЬНАЯ СКОРОСТЬ РЕКУПЕРАТОРА
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0033);
            }
        }.call();
        System.out.println("CL57R: Максимальная скорость 0x0033 = "+val.getValue() + " об/мин");

        // Число импульсов
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0034);
            }
        }.call();
        System.out.println("CL57R: Общее число импульсов в целевой позиции 0x0034 = "+val.getValue());

        // Модель привода
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0000);
            }
        }.call();
        System.out.println("CL57R: Модель привода 0x0000 = "+val.getValue());

        // Версия прошивки
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0001);
            }
        }.call();
        System.out.println("CL57R: Версия прошивки 0x0001 = "+val.getValue());

        // Текущая позиция
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0007);
            }
        }.call();
        System.out.println("CL57R: Текущая позиция 0x0007 = "+val.getValue());

        // ТЕКУЩАЯ СКОРОСТЬ РЕКУПЕРАТОРА
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0009);
            }
        }.call();
        System.out.println("CL57R: Скорость вращения 0x0009 = "+val.getValue() + " об/мин");

        // Режим работы
        // 0 - Нет операции ни в одном режиме
        // 1 - Режим местоположения
        // 2 - Скоростной режим
        // 3 - Возврат в нулевой режим
        // 4 - JOG режим
        // 5 - Многосегментное позиционирование
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0002);
            }
        }.call();
        System.out.println("CL57R: Режим работы 0x0002 = "+val.getValue());

        // Состояние
        // 0 - На месте
        // 1 - Возвращение в 0 завершено
        // 2 - Двигатель вращается
        // 3 - Тревога
        // 4 - Включение двигателя
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0003);
            }
        }.call();
        System.out.println("CL57R: Состояние 0x0003 = "+val.getValue());

        // Аварии
        // 0 - Перегрузка по току
        // 1 - Перенапряжение
        // 2 - Ошибка позиционирования
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit_MRC1,0x0004);
            }
        }.call();
        System.out.println("CL57R: Аварии 0x0004 = "+val.getValue());
        System.out.println();

        

        // --- Режим вентилирования ---
        // 0 - WMOff (Отключено)
        // 1 - WMCO2Eco (CO2 эконом)
        // 2 - WMCO2Norm (CO2 норма)
        // 3 - WMCO2Best (CO2 качество)
        // 4 - WMAirEco (Объём эконом)
        // 5 - WMAirNorm (Объём норма)
        // 6 - WMAirBest (Объём макс.)
        val = new APICall<JInt>() {
					@Override
					public Call<JInt> apiFun() {
							return api3.readESS2RegisterByLogUnit(token,logUnit2,0x0030);
					}
			}.call();
			System.out.println("AirPLC: Режим вентилирования 0x0030= "+val.getValue()); // 0 - отключено

			

			// Изменение режима вентилирования на эконом по CO2 (1)
			// Запись
			JEmpty vv111 = new APICall<JEmpty>() {
					@Override
					public Call<JEmpty> apiFun() {
							return api3.writeESS2RegisterByLogUnit(token,logUnit2,0x0030,1); // 1 - WMCO2Eco (CO2 эконом) 
					}
			}.call();

			
		}
}
