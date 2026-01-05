package com.example.modbus_api;

import okhttp3.OkHttpClient;
import com.example.modbus_api.modbus.model.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

import java.util.concurrent.TimeUnit;

public class Data {
    public final static int HTTPTimeOut = 60;                     
    public final static int HTTPOk = 200;
    public final static int HTTPAuthorization = 401;
    public final static int HTTPNotFound = 404;
    public final static int HTTPRequestError = 400;
    public final static int HTTPException = 500;
    public final static int HTTPServiceUnavailable = 503;  


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

        // ----------------------------- ДАННЫЕ СХЕМЫ ---------------------------------------------------------------
        // Температура в помещении (делить на 10)
        System.out.println("T5 = "  + (getParameter(api3, token, "AirPLC", 0x0140)) / 10.0);
        // Температура после нагревателя (делить на 10)
        System.out.println("T4 = "  + getParameter(api3, token, "AirPLC", 0x130) / 10.0);
        // Процент использования нагревателя
        //System.out.println("H1 = "  + getParameter(api3, token, "TCN6", 0x0003)); -- не работает
        // Текущий прокачиваемый объём (делить на 10)
        System.out.println("V = "   + getV(api3, token) / 10.0);
        // Относительная влажность в помещении (делить на 10)
        System.out.println("P = "   + getParameter(api3, token, "AirPLC", 0x0141) / 10.0);
        // % оборотов рекуператора от максимального
        System.out.println("R = "   + getR(api3,token));
        // КПД рекуператора (делить на 10)
        System.out.println("E = "   + getParameter(api3, token, "AirPLC", 0x0062) / 10.0);
        // Температура перед нагревателем (делить на 10)
        System.out.println("T3 = "  + getParameter(api3, token, "AirPLC", 0x120) / 10.0);
        // Объём прокачки вентилятора M1 (делить на 10)
        System.out.println("M1 = "  + getParameter(api3, token, "AirPLC", 0x0047) / 10.0);
        // % прокачки вентилятора M1
        System.out.println("M1% = " + getM1Percentage(api3, token));
        // Объём прокачки вентилятора M2 (делить на 10)
        System.out.println("M2 = "  + getParameter(api3, token, "AirPLC", 0x0048) / 10.0);
        // % прокачки вентилятора M2
        System.out.println("M2% = " + getM2Percentage(api3, token));

        // ------------------------------- MANUAL РЕЖИМЫ ----------------
        // Ползунок для manual-объёма
        System.out.println("Vman = "  + setParameter(api3, token, "AirPLC", 0x0024, 150));
        // Ползунок для manual-температуры
        System.out.println("Tman = "  + setParameter(api3, token, "AirPLC", 0x0025, 22));

        // -------------------------------- ПРАВЫЙ ВИДЖЕТ ОБЪЁМА ----------------------------------------------------
        // Уставки объёма
        System.out.println("ECO min = "  + setParameter(api3, token, "AirPLC", 0x0026, 400));
        System.out.println("ECO max = "  + setParameter(api3, token, "AirPLC", 0x0027, 800));
        System.out.println("Norm min = " + setParameter(api3, token, "AirPLC", 0x0028, 800));
        System.out.println("Norm max = " + setParameter(api3, token, "AirPLC", 0x0029, 1000));
        System.out.println("Intensive min = " + setParameter(api3, token, "AirPLC", 0x002A, 1000));
        System.out.println("Intensive max = " + setParameter(api3, token, "AirPLC", 0x002B, 1400));
        // Факторы скорости
        System.out.println("K1 = "  + setParameter(api3, token, "AirPLC", 0x0021, 80)); // ЭКО
        System.out.println("K2 = "  + setParameter(api3, token, "AirPLC", 0x0022, 100)); // НОРМ
        System.out.println("K3 = "  + setParameter(api3, token, "AirPLC", 0x0023, 120)); // ИНТЕНСИВ

        // -------------------------------- ПРАВЫЙ ВИДЖЕТ ТЕМПЕРАТУРЫ ----------------------------------------------------
        // x - текущая температура
        // y - целевая температура
        System.out.println("T1x = "  + setParameter(api3, token, "AirPLC", 0x0051, 15));
        System.out.println("T1y = "  + setParameter(api3, token, "AirPLC", 0x0050, 20));
        System.out.println("T1x = "  + setParameter(api3, token, "AirPLC", 0x0053, 20));
        System.out.println("T1y = "  + setParameter(api3, token, "AirPLC", 0x0052, 18));
        System.out.println("T1x = "  + setParameter(api3, token, "AirPLC", 0x0055, 22));
        System.out.println("T1y = "  + setParameter(api3, token, "AirPLC", 0x0054, 14));
        // четвёртой точки нет, но появится (если не обременительно - желательно временно убрать из таблицы)

    }

    public static int getParameter(RestAPIModbus api3, String token, final String logunit, final int address) throws UniException, InterruptedException {
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, logunit, address);
            }
        }.call();
        return val.getValue();
    }

    public static int setParameter(RestAPIModbus api3, String token, final String logunit, final int address, int value) throws UniException, InterruptedException {
        JEmpty vv = new APICall<JEmpty>() {
            @Override
            public Call<JEmpty> apiFun() {
                return api3.writeESS2RegisterByLogUnit(token, logunit, address, value);
            }
        }.call();
        return getParameter(api3, token, logunit, address);
    }

    // R - % от максимальной скорости рекуператора (чтение)
    public static int getR(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x005D);
            }
        }.call();
        int Rmin = val.getValue();

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x005E);
            }
        }.call();
        int Rmax = val.getValue();

        val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x005F);
            }
        }.call();
        int R = val.getValue();

        return (R - Rmin) * 100 / (Rmax - Rmin);
    }

    // Максимальный прокачиваемый объём (служебная функция для M и V, на экран не выводится)
    public static int getMaxV(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x0042);
            }
        }.call();
        return val.getValue();
    }

    // M1 - Объём M1 (чтение)
    public static int getM1(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x0047);
            }
        }.call();
        return val.getValue();
    }

    // M1% - Процентовка M1 (чтение)
    public static int getM1Percentage(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        // Отношение тееущего объёма M1 к максимально возможному
        return getM1(api3, token) * 100 / getMaxV(api3, token);
    }

    // M2 - Объём M2 (чтение)
    public static int getM2(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        JInt val = new APICall<JInt>() {
            @Override
            public Call<JInt> apiFun() {
                return api3.readESS2RegisterByLogUnit(token, "AirPLC", 0x0048);
            }
        }.call();
        return val.getValue();
    }

    // M2% - Процентовка M2 (чтение)
    public static int getM2Percentage(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        // Отношение тееущего объёма M2 к максимально возможному
        return getM2(api3, token) * 100 / getMaxV(api3, token);
    }

    // V - Текущий расход (max(M1,M2)) (чтение)
    public static int getV(RestAPIModbus api3, String token) throws UniException, InterruptedException {
        int M1 = getM1(api3, token);
        int M2 = getM2(api3, token);
        if (M1 > M2)
            return M1;
        else
            return M2;
    }

}
