TrailcamWeb project - ported to Java/Spring Boot/REST

Frontend remains a Vue/JavaScript solution, but as a PoC, I changed the backend. Instead of embedding the json data file, I now 
1) load json data from a URL, into the backend java service, on startup.
2) parse them with google-gson into java objects
3) provide a series of REST endpoints to serve the data to the Vue/JS frontend
4) tell Vue to retrieve data from the REST services via basic http get

This has changed the architecture to a typical microservices architecture, where data is served a-la-carte and on-demand to the web frontend.

This has the potential of introducing proprietary filtering, transformations into the backend services and not sharing all data/observations with the clients. It also helps once the data grows to a much larger data set, where each graph can be provided only the data needed to render the graph.

To test this code out, all you need to do is:

> git clone https://github.com/boaworm/TrailcamWeb-Spring.git
> 
> vi src/main/resources/static/locals.js (change the URL to wherever you run the backend. It can very well be localhost if you try it on a local machine)
>
> ./gradlew bootRun
>
Point your browser to port 8080 of whatever machine you are running this on, and you are up! In live/run mode, the service will pull the latest data files from my web server.

> [!NOTE]
> There are a series of tests you can run as well. They use embedded data samples. This allows for easy TDD.
>

> [!NOTE]
> I've only ported a small amount of function to use the REST services - this is more a PoC using Spring Boot than a full rewrite. It lacks any meaningful error handling, only representing a happy-path project.
> 
