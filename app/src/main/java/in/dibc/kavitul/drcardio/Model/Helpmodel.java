package in.dibc.kavitul.drcardio.Model;

public class Helpmodel
{
    /*Get Or Set Help Prameters */
    private String title, description;

    public Helpmodel(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }

}
