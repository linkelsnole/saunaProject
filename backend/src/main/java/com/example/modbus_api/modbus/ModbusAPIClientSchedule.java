package com.example.modbus_api.modbus;

import com.example.modbus_api.modbus.model.*;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ModbusAPIClientSchedule {
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
											String errorMessage = "";
										if (res.errorBody() != null) {
												try {
														errorMessage = res.errorBody().string();
												} catch (IOException e) {
														errorMessage = "Не удалось прочитать тело ошибки.";
												}
										}
										
										String finalMessage = "Ошибка " + res.message() + " (" + res.code() + ") " + errorMessage;

                    if (res.code()== HTTPAuthorization){
                        UniException.io("Сеанс закрыт " + finalMessage);
                    }
                    UniException.io(finalMessage);
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
        final String logUnit = "AirPLC";


        User user = new APICall<User>() {
            @Override
            public Call<User> apiFun() {
                return api3.login("9131111111","1234");
            }
        }.call();
        String token = user.getSessionToken();
        System.out.println("token="+token);

        //------------------------------------------------------------------------------------

        // ------ Расписание 1 ------

        // --- Режим расписания ---
        // 0 - Отключен
        // 1 - Фоновый
        // 2 - Поддержание
        // 3 - Дни недели/часы
        // 4 - Интервал - дни
        // 5 - Ночной - лето
        // 6 - Ночной - зима

        // Запись
        JEmpty vv1 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+0,0);
            }
        }.call();

        // Чтение
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+0);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Режим расписания 0x0400+0 = "+val.getValue());

        // --- Состояние ---
        // 0 - В работе
        // 1 - Перекрыт

        // Запись
        JEmpty vv2 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+1,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+1);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Состояние 0x0400+1 = "+val.getValue());

        // --- Маска дней недели ---
        // Запись
        JEmpty vv3 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+2,127);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+2);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Маска дней недели 0x0400+2 = "+val.getValue());

        // --- Дата 1 ---
        // Запись
        JEmpty vv4 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+3,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+3);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Дата 1 0x0400+3 = "+val.getValue());

        // --- Дата 2 ---
        // Запись
        JEmpty vv5 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+4,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+4);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Дата 2 0x0400+3 = "+val.getValue());

        // --- Время дня 1 ---
        // Запись
        JEmpty vv6 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+5,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+5);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Время дня 1 0x0400+5 = "+val.getValue());

        // --- Время дня 2 ---
        // Запись
        JEmpty vv7 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+6,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+6);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Время дня 2 0x0400+6 = "+val.getValue());

        // --- Продолжительность ---
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+13);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Продолжительность 0x0400+13 = "+val.getValue()+" сек");

        // Режим CO2
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv8 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+7,1);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+7);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Режим CO2 0x0400+7 = "+val.getValue());

        // Приоритет CO2
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv9 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+8,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+8);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Приоритет CO2 0x0400+8 = "+val.getValue());

        // Режим P
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv10 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+9,1);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+9);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Режим P 0x0400+9 = "+val.getValue());

        // Приоритет P
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv11 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+10,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+10);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Приоритет P 0x0400+10 = "+val.getValue());

        // Режим T
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv12 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+11,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+11);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Режим T 0x0400+11 = "+val.getValue());

        // Приоритет T
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv13 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x400+12,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0400+12);
            }
        }.call();
        System.out.println("AirPLC Schedule 1: Приоритет T 0x0400+12 = "+val.getValue());

        System.out.println();

        // ------ Расписание 2 ------

        // --- Режим расписания ---
        // 0 - Отключен
        // 1 - Фоновый
        // 2 - Поддержание
        // 3 - Дни недели/часы
        // 4 - Интервал - дни
        // 5 - Ночной - лето
        // 6 - Ночной - зима

        // Запись
        JEmpty vv21 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+0,3);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+0);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Режим расписания 0x0410+0 = "+val.getValue());

        // --- Состояние ---
        // 0 - В работе
        // 1 - Перекрыт

        // Запись
        JEmpty vv22 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+1,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+1);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Состояние 0x0410+1 = "+val.getValue());

        // --- Маска дней недели ---
        // Запись
        JEmpty vv23 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+2,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+2);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Маска дней недели 0x0410+2 = "+val.getValue());

        // --- Дата 1 ---
        // Запись
        JEmpty vv24 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+3,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+3);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Дата 1 0x0410+3 = "+val.getValue());

        // --- Дата 2 ---
        // Запись
        JEmpty vv25 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+4,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+4);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Дата 2 0x0410+3 = "+val.getValue());

        // --- Время дня 1 ---
        // Запись
        JEmpty vv26 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+5,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+5);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Время дня 1 0x0410+5 = "+val.getValue());

        // --- Время дня 2 ---
        // Запись
        JEmpty vv27 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+6,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+6);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Время дня 2 0x0410+6 = "+val.getValue());

        // --- Продолжительность ---
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+13);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Продолжительность 0x0410+13 = "+val.getValue()+" сек");

        // Режим CO2
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv28 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+7,2);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+7);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Режим CO2 0x0410+7 = "+val.getValue());

        // Приоритет CO2
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv29 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+8,2);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+8);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Приоритет CO2 0x0410+8 = "+val.getValue());

        // Режим P
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv210 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+9,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+9);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Режим P 0x0410+9 = "+val.getValue());

        // Приоритет P
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv211 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+10,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+10);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Приоритет P 0x0410+10 = "+val.getValue());

        // Режим T
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv212 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+11,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+11);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Режим T 0x0410+11 = "+val.getValue());

        // Приоритет T
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv213 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x410+12,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0410+12);
            }
        }.call();
        System.out.println("AirPLC Schedule 2: Приоритет T 0x0410+12 = "+val.getValue());
        System.out.println();

        // ------ Расписание 3 ------

        // --- Режим расписания ---
        // 0 - Отключен
        // 1 - Фоновый
        // 2 - Поддержание
        // 3 - Дни недели/часы
        // 4 - Интервал - дни
        // 5 - Ночной - лето
        // 6 - Ночной - зима

        // Запись
        JEmpty vv31 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+0,2);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+0);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Режим расписания 0x0420+0 = "+val.getValue());

        // --- Состояние ---
        // 0 - В работе
        // 1 - Перекрыт

        // Запись
        JEmpty vv32 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+1,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+1);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Состояние 0x0420+1 = "+val.getValue());

        // --- Маска дней недели ---
        // Запись
        JEmpty vv33 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+2,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+2);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Маска дней недели 0x0420+2 = "+val.getValue());

        // --- Дата 1 ---
        // Запись
        JEmpty vv34 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+3,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+3);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Дата 1 0x0420+3 = "+val.getValue());

        // --- Дата 2 ---
        // Запись
        JEmpty vv35 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+4,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+4);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Дата 2 0x0420+3 = "+val.getValue());

        // --- Время дня 1 ---
        // Запись
        JEmpty vv36 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+5,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+5);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Время дня 1 0x0420+5 = "+val.getValue());

        // --- Время дня 2 ---
        // Запись
        JEmpty vv37 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+6,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+6);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Время дня 2 0x0420+6 = "+val.getValue());

        // --- Продолжительность ---
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+13);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Продолжительность 0x0420+13 = "+val.getValue()+" сек");

        // Режим CO2
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv38 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+7,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+7);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Режим CO2 0x0420+7 = "+val.getValue());

        // Приоритет CO2
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv39 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+8,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+8);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Приоритет CO2 0x0420+8 = "+val.getValue());

        // Режим P
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv310 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+9,3);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+9);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Режим P 0x0420+9 = "+val.getValue());

        // Приоритет P
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv311 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+10,3);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+10);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Приоритет P 0x0420+10 = "+val.getValue());

        // Режим T
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv312 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+11,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+11);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Режим T 0x0420+11 = "+val.getValue());

        // Приоритет T
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv313 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x420+12,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0420+12);
            }
        }.call();
        System.out.println("AirPLC Schedule 3: Приоритет T 0x0420+12 = "+val.getValue());
        System.out.println();

        // ------ Расписание 4 ------

        // --- Режим расписания ---
        // 0 - Отключен
        // 1 - Фоновый
        // 2 - Поддержание
        // 3 - Дни недели/часы
        // 4 - Интервал - дни
        // 5 - Ночной - лето
        // 6 - Ночной - зима

        // Запись
        JEmpty vv41 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+0,2);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+0);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Режим расписания 0x0430+0 = "+val.getValue());

        // --- Состояние ---
        // 0 - В работе
        // 1 - Перекрыт

        // Запись
        JEmpty vv42 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+1,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+1);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Состояние 0x0430+1 = "+val.getValue());

        // --- Маска дней недели ---
        // Запись
        JEmpty vv43 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+2,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+2);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Маска дней недели 0x0430+2 = "+val.getValue());

        // --- Дата 1 ---
        // Запись
        JEmpty vv44 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+3,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+3);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Дата 1 0x0430+3 = "+val.getValue());

        // --- Дата 2 ---
        // Запись
        JEmpty vv45 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+4,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+4);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Дата 2 0x0430+3 = "+val.getValue());

        // --- Время дня 1 ---
        // Запись
        JEmpty vv46 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+5,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+5);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Время дня 1 0x0430+5 = "+val.getValue());

        // --- Время дня 2 ---
        // Запись
        JEmpty vv47 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+6,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+6);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Время дня 2 0x0430+6 = "+val.getValue());

        // --- Продолжительность ---
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+13);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Продолжительность 0x0430+13 = "+val.getValue()+" сек");

        // Режим CO2
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv48 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+7,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+7);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Режим CO2 0x0430+7 = "+val.getValue());

        // Приоритет CO2
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv49 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+8,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+8);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Приоритет CO2 0x0430+8 = "+val.getValue());

        // Режим P
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv410 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+9,3);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+9);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Режим P 0x0430+9 = "+val.getValue());

        // Приоритет P
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv411 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+10,3);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+10);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Приоритет P 0x0430+10 = "+val.getValue());

        // Режим T
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv412 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+11,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+11);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Режим T 0x0430+11 = "+val.getValue());

        // Приоритет T
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv413 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x430+12,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0430+12);
            }
        }.call();
        System.out.println("AirPLC Schedule 4: Приоритет T 0x0430+12 = "+val.getValue());
        System.out.println();

        // ------ Расписание 5 ------

        // --- Режим расписания ---
        // 0 - Отключен
        // 1 - Фоновый
        // 2 - Поддержание
        // 3 - Дни недели/часы
        // 4 - Интервал - дни
        // 5 - Ночной - лето
        // 6 - Ночной - зима

        // Запись
        JEmpty vv51 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+0,2);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+0);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Режим расписания 0x0440+0 = "+val.getValue());

        // --- Состояние ---
        // 0 - В работе
        // 1 - Перекрыт

        // Запись
        JEmpty vv52 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+1,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+1);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Состояние 0x0440+1 = "+val.getValue());

        // --- Маска дней недели ---
        // Запись
        JEmpty vv53 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+2,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+2);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Маска дней недели 0x0440+2 = "+val.getValue());

        // --- Дата 1 ---
        // Запись
        JEmpty vv54 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+3,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+3);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Дата 1 0x0440+3 = "+val.getValue());

        // --- Дата 2 ---
        // Запись
        JEmpty vv55 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+4,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+4);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Дата 2 0x0440+3 = "+val.getValue());

        // --- Время дня 1 ---
        // Запись
        JEmpty vv56 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+5,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+5);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Время дня 1 0x0440+5 = "+val.getValue());

        // --- Время дня 2 ---
        // Запись
        JEmpty vv57 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+6,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+6);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Время дня 2 0x0440+6 = "+val.getValue());

        // --- Продолжительность ---
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+13);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Продолжительность 0x0440+13 = "+val.getValue()+" сек");

        // Режим CO2
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv58 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+7,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+7);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Режим CO2 0x0440+7 = "+val.getValue());

        // Приоритет CO2
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv59 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+8,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+8);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Приоритет CO2 0x0440+8 = "+val.getValue());

        // Режим P
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv510 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+9,3);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+9);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Режим P 0x0440+9 = "+val.getValue());

        // Приоритет P
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv511 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+10,3);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+10);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Приоритет P 0x0440+10 = "+val.getValue());

        // Режим T
        // 0 - Отключен
        // 1 - Эконом
        // 2 - Норма
        // 3 - Интенсивный
        // Запись
        JEmpty vv512 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+11,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+11);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Режим T 0x0440+11 = "+val.getValue());

        // Приоритет T
        // 0 - Отключен
        // 1 - Низкий
        // 2 - Средний
        // 3 - Высокий
        // Запись
        JEmpty vv513 = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token,logUnit,0x440+12,0);
            }
        }.call();

        // Чтение
        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token,logUnit,0x0440+12);
            }
        }.call();
        System.out.println("AirPLC Schedule 5: Приоритет T 0x0440+12 = "+val.getValue());
    }
}
