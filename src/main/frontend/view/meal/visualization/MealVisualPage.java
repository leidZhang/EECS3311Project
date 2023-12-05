package main.frontend.view.meal.visualization;

import main.backend.common.Result;
import main.backend.food.entity.Nutrient;
import main.backend.meal.IMealController;
import main.backend.meal.impl.MealController;
import main.backend.user.entity.User;
import main.frontend.common.Content;
import main.frontend.common.ContentBuilder;
import main.frontend.common.Director;
import main.frontend.custom.entry.NfEntry;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

public class MealVisualPage extends Content {
    private final String DEFAULT_CATEGORY = "Your Intake";
    private Content recordPage;
    private IMealController controller = new MealController();
    private Map<String, JButton> buttonMap;
    private Map<String, NfEntry> entries;
    private DefaultPieDataset pieDataset;
    private DefaultCategoryDataset radarDataSet;
    private final String CFG_RECOMMEND = "CFG Recommendation";
    private final Object[][] CFG_RECOMMENDATION = new Object[][]{
            {50.0f, "Vegetables and Fruits"},
            {25.0f, "Protein"},
            {25.0f, "Whole Grain"}
    };

    public MealVisualPage(Content recordPage) {
        this.recordPage = recordPage;
    }

    private ActionListener handleViewRecords() {
        return e -> {
           frontEnd.get().switchContentPanel(recordPage);
        };
    }

    private ActionListener handleApply() {
        return e -> {
            if (!verifyEntries()) return;
            pieDataset.clear(); // clear dataset
            radarDataSet.clear(); // clear dataset

            Date startDate = Date.valueOf(entries.get("Start Date").getInput());
            Date endDate = Date.valueOf(entries.get("End Date").getInput());

            renderPieChart(startDate, endDate);
            renderRadarChart(startDate, endDate);
        };
    }

    private void  handleDefaultRender() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(14);
        renderPieChart(Date.valueOf(start), Date.valueOf(today));
        renderRadarChart(Date.valueOf(start), Date.valueOf(today));
    }

    private ActionListener handleReset() {
        return e -> {
            for (Map.Entry<String, NfEntry> entry : entries.entrySet()) {
                entry.getValue().setEntry("");
                entry.getValue().setMessage("");
            }

            handleDefaultRender();
        };
    }

    private boolean verifyEntries() {
        boolean flag = true;
        for (Map.Entry<String, NfEntry> entry : entries.entrySet()) {
            entry.getValue().setMessage("");
            flag = flag & entry.getValue().verifyInput();
        }

        return flag;
    }

    private void setUpEntries() {
        for (Map.Entry<String, NfEntry> entry : entries.entrySet()) {
            entry.getValue().setRegex(
                "^(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$",
                "data input format should be yyyy-mm-dd"
            );
        }
    }

    private void renderPieChart(Date startDate, Date endDate) {
        User user = instance.getUser();

        Result res = controller.getSortedDailyNutrient(user, startDate, endDate);
        if (res.getCode().equals("200")) {
            pieDataset.clear(); // clear every time
            Map<Nutrient, Float> map = (Map<Nutrient, Float>) res.getData();

            for (Map.Entry<Nutrient, Float> entry : map.entrySet()) {
                Nutrient nutrient = entry.getKey();
                String tag = nutrient.getName();
                float value = entry.getValue();

                pieDataset.setValue(tag, value);
            }
        } else {
            JOptionPane.showMessageDialog(null, res.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderRadarChart(Date startDate, Date endDate) {
        User user = instance.getUser();

        for (Object[] data : CFG_RECOMMENDATION) {
            radarDataSet.setValue((float) data[0], CFG_RECOMMEND, (String) data[1]);
        }

        Result res = controller.getMealGroups(user, startDate, endDate);
        if (res.getCode().equals("200")) {
            Map<String, Float> map = (Map<String, Float>) res.getData();

            for (Map.Entry<String, Float> entry : map.entrySet()) {
                String tag = entry.getKey();
                float value = entry.getValue();

                radarDataSet.setValue(value, DEFAULT_CATEGORY, tag);
            }
        } else {
            JOptionPane.showMessageDialog(null, res.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void mount(JPanel content) {
        buttonMap.get("Apply").addActionListener(handleApply());
        buttonMap.get("Reset").addActionListener(handleReset());
        buttonMap.get("View Records").addActionListener(handleViewRecords());
        setUpEntries();

        handleDefaultRender();
    }

    @Override
    public String showContent(JPanel content) {
        MealVisualBuilder builder = new MealVisualBuilder(content);
        Director director = new Director(builder);

        director.constructPage("Meal Visualization");
        buttonMap = builder.getButtonMap();
        entries = builder.getEntries();
        pieDataset = builder.getPieDataSet();
        radarDataSet = builder.getRadarPlot();

        mount(content);

        return "Meal Visualization";
    }
}
