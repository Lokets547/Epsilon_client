package wtf.dettex.api.repository.theme;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Theme {
    private String name;
    private int primaryColor;
    private int secondaryColor;
    private int backgroundColor;
    private int moduleColor;
    private int settingColor;
    private int textColor;
}
