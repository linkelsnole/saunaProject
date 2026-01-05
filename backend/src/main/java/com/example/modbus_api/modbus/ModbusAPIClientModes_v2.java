package com.example.modbus_api.modbus;

import com.example.modbus_api.modbus.model.*;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ModbusAPIClientModes_v2 {
    public final static int HTTPTimeOut = 60;                     // Тайм-аут клиента
    public final static int HTTPOk = 200;
    public final static int HTTPAuthorization = 401;
    public final static int HTTPNotFound = 404;
    public final static int HTTPRequestError = 400;
    public final static int HTTPException = 500;
    public final static int HTTPServiceUnavailable = 503;  // Сервис недоступен

    //-------------------------------------------------------------------------------------------------------
    public abstract static class APICall<T> {
        public abstract Call<T> apiFun();

        public T call() throws UniException {
            try {
                Response<T> res = apiFun().execute();
                if (!res.isSuccessful()) {
                    if (res.code() == HTTPAuthorization) {
                        UniException.io("Сеанс закрыт " + Utils.httpError(res));
                    }
                    UniException.io("Ошибка " + res.message() + " (" + res.code() + ") " + res.errorBody().string());
                }
                return res.body();
            } catch (Exception ex) {
                throw UniException.io(ex);
            }
        }
    }

    //-------------------------------------------------------------------------------------------------------
    public interface RestAPIModbus {
        /**
         * Активность клиента - в ответе - количество неподтвержденных уведомлений
         */
        @GET("/api/keepalive")
        Call<JInt> keepalive(@Header("SessionToken") String token);

        /**
         * Авторизация по номеру телефона (логину) и паролю
         */
        @GET("/api/user/login/phone")
        Call<User> login(@Query("phone") String phone, @Query("pass") String pass);

        /**
         * Прочитать регистр ПЛК по номеру
         */
        @GET("/api/ess2/register/value/read")
        Call<JInt> readESS2RegisterValue(@Header("SessionToken") String token, @Query("deviceName") String deviceName, @Query("unit") int unit, @Query("regnum") int regnum, @Query("equipControl") boolean equipControl);

        /**
         * Записать регистр ПЛК по номеру
         */
        @POST("/api/ess2/register/value/write")
        Call<JEmpty> writeESS2RegisterValue(@Header("SessionToken") String token, @Query("deviceName") String deviceName, @Query("unit") int unit, @Query("regnum") int regnum, @Query("value") int value, @Query("equipControl") boolean equipControl);

        /**
         * Прочитать регистры device по заданному списку
         */
        @POST("/api/ess2/registers/list/read")
        Call<IntegerList> readESS2RegistersValues(@Header("SessionToken") String token, @Query("deviceName") String deviceName, @Body UnitRegisterList list);

        /**
         * Прочитать регистр ПЛК по номеру
         */
        @GET("/api/ess2/register/logunit/read")
        Call<JInt> readESS2RegisterByLogUnit(@Header("SessionToken") String token, @Query("logName") String deviceName, @Query("regnum") int regnum);

        /**
         * Записать регистр ПЛК по номеру
         */
        @POST("/api/ess2/register/logunit/write")
        Call<JEmpty> writeESS2RegisterByLogUnit(@Header("SessionToken") String token, @Query("logName") String deviceName, @Query("regnum") int regnum, @Query("value") int value);

        /**
         * Прочитать регистры device по заданному списку
         */
        @POST("/api/ess2/register/list/logunit/read")
        Call<IntegerList> readESS2RegisterListByLogUnit(@Header("SessionToken") String token, @Query("logName") String deviceName, @Body UnitRegisterList list);
    }

    public static Object startClient(String ip, String port, Class restAPI) throws UniException {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(HTTPTimeOut, TimeUnit.SECONDS)
                .connectTimeout(HTTPTimeOut, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://" + ip + ":" + port)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(restAPI);
    }

    public static void main(String ss[]) throws UniException, InterruptedException {

        final String ip = "172.16.6.119";
        //final String ip = "localhost";
        final String port = "4567";
        final RestAPIModbus api3 = (RestAPIModbus) startClient(ip, port, RestAPIModbus.class);    // Короткое API для регистров Modbus
        final String sensorDevice = "Sensors";
        final String controlDevice = "AirPLC";
        final String logUnit2 = "AirPLC";
        final String logUnit = "T1";
        User user = new APICall<User>() {
            @Override
            public Call<User> apiFun() {
                return api3.login("9131111111", "1234");
            }
        }.call();
        String token = user.getSessionToken();
        System.out.println("token=" + token);

        int x = start(api3, token);
        int y = changeMode(api3, token, 0, 0);
        int z = stop(api3, token);

    }

    public static int start(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        // Команда запуска агрегата
        JEmpty vv = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token, "AirPLC", 0x012, 1); // 1 - пуск агрегата
            }
        }.call();

        // Чтение текущего статуса на протяжении 40 секунд
        int status = 0;
        for (int i = 0; i < 20; i++) {
            JInt val = new APICall<JInt>() {
                @Override
                public Call<JInt> apiFun() {
                    return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x0010);
                }
            }.call();
            status = val.getValue();
            //System.out.println(status); // лог текущего состояния
            if ((status & 64) == 64) // 64 = 2^6 (бит working)
                return status; // возврат статуса запущенного агрегата (>64)
            TimeUnit.SECONDS.sleep(2);
        }
        // если за 40 секунд агрегат не запустился - останавливаем его
        return stop(api3, token);
    }

    public static int quickStart(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        // Команда запуска агрегата
        JEmpty vv = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token, "AirPLC", 0x012, 6); // 1 - быстрый запуск агрегата
            }
        }.call();

        // Чтение текущего статуса на протяжении 40 секунд
        int status = 0;
        for (int i = 0; i < 40; i++) {
            JInt val = new APICall<JInt>() {
                @Override
                public Call<JInt> apiFun() {
                    return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x0010);
                }
            }.call();
            status = val.getValue();
            //System.out.println(status); // лог текущего состояния
            if ((status & 64) == 64) // 64 = 2^6 (бит working)
                return status;
            TimeUnit.SECONDS.sleep(1);
        }
        // если за 40 секунд агрегат не запустился - останавливаем его
        return stop(api3, token);
    }

    public static int stop(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        // Команда остановки агрегата
        JEmpty vv = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token, "AirPLC", 0x012, 2); // 2 - остановка агрегата
            }
        }.call();

        // Чтение текущего статуса на протяжении 20 секунд
        int status = 0;
        for (int i = 0; i < 20; i++) {
            JInt val = new APICall<JInt>() {
                @Override
                public Call<JInt> apiFun() {
                    return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x0010);
                }
            }.call();
            status = val.getValue();
            //System.out.println(status); // лог текущего состояния
            if (status == 1) // 1 = 2^0 (бит off)
                break;
            TimeUnit.SECONDS.sleep(1);
        }
        // возвращает статус остановленного агрегата (1) или другой статус, если за 20 секунд агрегат не остановился
        return status;
    }

    public static int changeMode(RestAPIModbus api3, String token, int Pmode, int Tmode) throws UniException, InterruptedException {
        // Проверка текущего состояния агрегата
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x0010);
            }
        }.call();
        if (val.getValue() < 8) // агрегат выключен или наблюдаются ошибки
            return -1; // режим переключить невозможно

        // поиск расписания userSet
        for (int i = 0; i < 10; i++) {
            final int t = i;
            val = new APICall<JInt>() {
                @Override
                public Call<JInt> apiFun() {
                    return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x0400 + t * 16); // 410 = 400 + 16 в x16
                }
            }.call();
            // Если найден userSet (режим по кнопкам "эко-норм-интенсив / эко-авто)
            if (val.getValue() == 8) {
                // Назначение состояния расписанию с кнопками
                // Если обе кнопки отжаты - unworking, unselected (0)
                // Если хотя бы одна кнопка нажата - working, selected (17 = 0..011 в x16)
                int ws  = Pmode + Tmode > 0 ? 17 : 0;
                JEmpty vv = new APICall<JEmpty>() {
                    @Override
                    public Call<JEmpty> apiFun() {
                        return api3.writeESS2RegisterByLogUnit(token,"AirPLC",0x0400 + t * 16 + 1, ws);
                    }
                }.call();
                // ToDo проверить, что working и selected установились

                // Смена режима вентилирования UserSet на переданный (0 - Отключён, 1 - Эко, 2 - Норм, 3 - Интенсив)
                vv = new APICall<JEmpty>() {
                    @Override
                    public Call<JEmpty> apiFun() {
                        // regnum + 9 - Pmode
                        return api3.writeESS2RegisterByLogUnit(token,"AirPLC",0x0400 + t * 16 + 9, Pmode);
                    }
                }.call();
                // ToDo проверить, что Pmode установился

                // Смена температурного режима UserSet на переданный (0 - Отключён, 1 - Эко, 2 - Авто)
                vv = new APICall<JEmpty>() {
                    @Override
                    public Call<JEmpty> apiFun() {
                        // regnum + 9 - Pmode
                        return api3.writeESS2RegisterByLogUnit(token,"AirPLC",0x0400 + t * 16 + 10, Tmode);
                    }
                }.call();
                // ToDo проверить, что Tmode установился

                // Попытка быстрого запуска агрегата и возврат его текущего состояния
                return quickStart(api3, token);
            }
        }
        // Если userSet среди расписаний нет - поменять режим невозможно
        return -3;
    }
}
