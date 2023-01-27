import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;

public class Main {

    public static String getAnimeListbySeason(int year, String season, int page){//вызываем и получаем http request
        HttpResponse<String> response;//ответ, содержащий строку, из котором будем извлекать json объекты
        try {
            HttpClient client = HttpClient.newBuilder()//создание HttpClient client
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()//создание запроса
                    .uri(new URI("https://api.jikan.moe/v4/seasons/"+year+"/"+season+"?page="+page))
                    .GET()
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response.body();
    }

    public static boolean parseAnime (String generaljson, int year, String season) {//парсим страницу, содержащую аниме в некотором сезоне некоторого года
        FileWriter fileWriter = null;
        PrintWriter printWriter = null;

        Boolean pagenext = null;//для проверки существования след стр, так как миксимум 25 аниме на стр, а их обычно больше
        String genre = null;//"главный" жанр
        String studioname = null;//название студии-создателя
        String anime_score = null;//рейтинг аниме
        String id_anime = null;//id аниме (нужно для получения рейтинга, жанра, названия студии)
        String themes = null;
        String rating = null;
        String demographic = null;
        String anime_name = null;

        // Считываем json
        try {
            fileWriter = new FileWriter("rawdata/"+year + season + ".csv",true);//создание файла, в которые записываются данные
            printWriter = new PrintWriter(fileWriter);
            String colomnsname =  "id_anime" + ";"  + "anime_name" + ";" + "anime_score" + ";" + "studioname" + ";" + "genre" + ";" +"themes" + ";" + "demographic" + ";" + "rating" + "\n";
            printWriter.print (colomnsname);
            printWriter.flush();
            JSONObject obj = (JSONObject) new JSONParser().parse(generaljson);//парсим json объект из переданной строки(список всех аниме в сезоне)
            // Достаем массив номеров
            JSONArray animeArr = (JSONArray) obj.get("data");//создание массива, содержащие данные из data
            JSONObject paginationList = (JSONObject) obj.get("pagination");//объект, содержащий информацию о способе отображения произведений

            try {//проверяем, существует ли следующая страница в списке
                pagenext = (Boolean) paginationList.get("has_next_page");
            }catch(Exception ex){
                System.out.println("PAGELIST "+obj);
            }
            Iterator animeItr = animeArr.iterator();//итератор по массиву всех произведений в списке
            // Выводим в цикле данные массива
            while (animeItr.hasNext()) {//пока существует след элемент
                JSONObject test = (JSONObject) animeItr.next();//создаем json объект для конкретного произв, на котором находится итератор
                JSONArray studioArr = (JSONArray) test.get("studios");//список всех студий
                studioname = "";
                if(studioArr.size()==0){//если массив пуст, то студия неизвестна
                    studioname = "UNKNOWN";
                }else{//в противном случае, считаем за "главную" студию первую в списке
                    JSONObject st = (JSONObject) studioArr.get(0);//объект, содержащий всю инфо о студии
                    studioname = (String) st.get("name");//достаем название студии
                }
                JSONArray genreArr = (JSONArray) test.get("genres");//список всех жанров
                genre = "";
                if(genreArr.size()==0){//если список пуст, то жанр считается неизвестным
                    genre = "UNKNOWN";
                }else {//в противном случае, считаем за "главный" жанр первый в списке
                    for (int i = 0; i < genreArr.size(); i++) {
                        JSONObject gn = (JSONObject) genreArr.get(i);//объект, содержащий всю инфо о жанре
                        if (i == genreArr.size()-1) {
                            genre += (String) gn.get("name");//достаем название жанра
                        }
                        else {
                            genre += (String) gn.get("name")+ ", ";
                        }
                    }
                }//проверка, что у произв есть рейтинг
                if(test.get("score")!=null){
                    String className = test.get("score").getClass().getName();//узнаем тип данных, соотв полученному рейтингу
                    if(className.indexOf("Double")>=0) {//когда рейтинг не целочисленный
                        anime_score = Double.toString((Double) test.get("score"));
                    }else if(className.indexOf("Long")>=0){//т. к. возможна ситуация, когда рейтинг целочисленный
                        anime_score = Long.toString((Long) test.get("score"));
                    }else{
                        anime_score = null;
                    }
                }else {
                    anime_score = null;
                }

                //anime_name = (String) test.get("name");
                JSONArray titles = (JSONArray) test.get("titles");
                if (titles!=null && titles.size() > 0) {
                    anime_name = ((JSONObject) titles.get(0)).get("title").toString();
                }
                else {
                    anime_name = "UNKNOWN";
                }

                JSONArray themesArr = (JSONArray) test.get("themes");
                themes = "";
                if (themesArr.size() == 0) {
                    themes = "UNKNOWN";
                } else {
                    for (int i = 0; i < themesArr.size(); i++) {
                        JSONObject th = (JSONObject) themesArr.get(i);
                        if (i == themesArr.size()-1) {
                            themes += (String) th.get("name");
                        }
                        else {themes += (String) th.get("name") + ", ";}
                    }
                }

                //demographic = (String) test.get("demographics");
                JSONArray dem = (JSONArray) test.get("demographics");
                if (dem!=null && dem.size() > 0) {
                    demographic = ((JSONObject) dem.get(0)).get("name").toString();
                }
                else {
                    demographic = "UNKNOWN";
                }
                rating = (String) test.get("rating");

                id_anime = Long.toString((Long)test.get("mal_id"));//достаем id аниме из json объекта
                System.out.println("- id: " + id_anime + "  - score: " + anime_score +
                        "  - studio: " + studioname + "  - genre: " + genre + "  - demographics: " + demographic + "  - rating: " + rating);

                try {//останавливаем поток запросов на 1 с, т.к. макс кол-во запросов 60 в минуту
                    Thread.sleep(1100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //если все поля непусты и известны, то записываем данные в файл
                if ((anime_score != null) && (studioname.equalsIgnoreCase("UNKNOWN")==false) && (genre.equalsIgnoreCase("UNKNOWN")==false)
                        && (themes.equalsIgnoreCase("UNKNOWN")==false) && (demographic.equalsIgnoreCase("UNKNOWN")==false) && (rating != null)) {
                    String finalLine =  id_anime + ";" +anime_name + ";" + anime_score + ";" + studioname + ";" + genre + ";" +themes + ";" + demographic + ";" + rating + "\n";
                    printWriter.print (finalLine);
                    printWriter.flush();
                }
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {//чтобы закрыть файл в любом случае, даже если он пустой
            if (printWriter != null) {
                printWriter.flush();
                printWriter.close();
            }
        }
        return pagenext;
    }

    public static void main(String[] args) {

//        String list = getAnimeListbySeason(2022, "fall", 1);
//        //System.out.println(list);
//        parseAnime(list, 2022, "fall");

        boolean nextpage = true;//проверка существует ли следующая страница(всего на стр 25 элементов)
        int page = 1;//номер страницы, с которой парсируем эелементы
        String[] seasons = {"winter", "spring", "summer", "fall"};

        for (int i = 2001; i < 2023; i++ ) {//просматриваем все тайтлы с периода 2015 по 2022 включительно
            for (int j = 0; j < 4; j++) {
                while (nextpage == true) {//пока существует след стр
                    String fullList = getAnimeListbySeason(i, seasons[j], page);//получаем всю информацию об аниме по году и сезону
                    nextpage = parseAnime(fullList, i, seasons[j]);//парсим информацию про аниме и получаем: id, score, name studio, main genre
                    page++;//возвращаем true/false в зависимости от существования след стр и переходим на след стр

                    try {//останавливаем поток запросов на 1 с, т.к. макс кол-во запросов 60 в минуту
                        Thread.sleep(1100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                nextpage = true;//сбрасываем страницу для каждого года
                page = 1;
            }
        }
    }
}