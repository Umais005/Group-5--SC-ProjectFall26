import java.util.ArrayList;
import java.util.List;

/** Data of ONE screen: its own facade and acting user. Screens never share an AppContext. */
class AppContext {
    final DBFacade db = new DBFacade();
    final EcoDriveFacade facade = new EcoDriveFacade(db);
    private User currentUser;
    private final List<Runnable> refreshers = new ArrayList<>();

    User user() { return currentUser; }
    void setUser(User u) { currentUser = u; }
    void addRefresher(Runnable r) { refreshers.add(r); }
    void refreshScreens() { for (Runnable r : refreshers) r.run(); }
}
