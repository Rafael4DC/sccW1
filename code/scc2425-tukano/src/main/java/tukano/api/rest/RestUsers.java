package tukano.api.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tukano.api.User;

import java.util.List;

@Path(RestUsers.PATH)
public interface RestUsers {

    String PATH = "/users";

    String PWD = "pwd";
    String QUERY = "query";
    String USER_ID = "userId";

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createUser(User user);


    @GET
    @Path("/{" + USER_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    User getUser(@PathParam(USER_ID) String userId, @QueryParam(PWD) String pwd);


    @PUT
    @Path("/{" + USER_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    User updateUser(@PathParam(USER_ID) String userId, @QueryParam(PWD) String pwd, User user);


    @DELETE
    @Path("/{" + USER_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    User deleteUser(@PathParam(USER_ID) String userId, @QueryParam(PWD) String pwd);


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<User> searchUsers(@QueryParam(QUERY) String pattern);
}
