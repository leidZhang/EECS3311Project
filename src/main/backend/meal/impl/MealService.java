package main.backend.meal.impl;

import main.backend.common.PeriodValidator;
import main.backend.food.IFoodService;
import main.backend.food.entity.Food;
import main.backend.food.entity.Nutrient;
import main.backend.food.impl.FoodService;
import main.backend.food.util.GroupConverter;
import main.backend.meal.IMealMapper;
import main.backend.meal.IMealService;
import main.backend.meal.entity.Meal;
import main.backend.meal.util.MealProcessor;
import main.backend.meal.util.MealValidator;
import main.backend.meal.util.NutrientCalculator;
import main.backend.user.entity.User;
import main.backend.validator.impl.IdValidator;

import java.sql.SQLException;
import java.sql.Date;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealService implements IMealService {
    private IMealMapper mapper = new MealMapper();
    private IFoodService service = new FoodService();
    private MealProcessor util = new MealProcessor(); // replace it with interface
    private NutrientCalculator calculator = new NutrientCalculator(); // replace it with interface
    private GroupConverter converter = new GroupConverter(); // replace it with interface

    private int calCalories(Map<Food, Float> foodMap) throws SQLException {
        int res = 0;
        for (Map.Entry<Food, Float> entry : foodMap.entrySet()) {
            int id = entry.getKey().getId();
            float weight = entry.getValue();
            res += service.getUnitCalories(id) * weight / 100;
        }

        return res;
    }

    private float calOthers(Map<Food, Float> foodMap, float vitaminValue, float carbValue, float proteinValue) {
        float total = 0;
        for (Map.Entry<Food, Float> entry : foodMap.entrySet()) {
            total += entry.getValue();
        }

        return total - vitaminValue - carbValue - proteinValue;
    }

    private void validateDuplicate(Date date, User user, String type) throws SQLException, IllegalArgumentException {
        if (type.equals("Snack")) return;
        Meal meal = mapper.getByDateAndType(date, user, type);
        if (meal != null) throw new IllegalArgumentException("Duplicate meal today!");
    }

    @Override
    public void save(Meal meal, User user) throws SQLException, IllegalArgumentException {
        MealValidator validator = new MealValidator(meal);
        validator.validate();
        validateDuplicate(meal.getDate(), user, meal.getType());

        // Meal need to calculate general nutrition
        Map<Food, Float> foodMap = meal.getFoodMap(); // we can load food map in front end
        Map<Nutrient, Float> mealNutrientMap = util.getNutrientMap(foodMap); // raw nutrient map

        // cal nutrients
        int totalCalories = calCalories(foodMap);
        float vitaminValue = calculator.getTotalVitamin(mealNutrientMap);
        float carbValue = calculator.getTotalCarbs(mealNutrientMap);
        float proteinValue = calculator.getTotalProtein(mealNutrientMap);
        float otherValue = calOthers(foodMap, vitaminValue, carbValue, proteinValue);
        // set nutrients
        meal.setTotalVitamins(vitaminValue);
        meal.setTotalProtein(proteinValue);
        meal.setTotalCarbs(carbValue);
        meal.setTotalOthers(otherValue);
        meal.setTotalCalories(totalCalories);

        mapper.save(meal, user);
    }

    private void validateId(int id) {
        IdValidator validator = new IdValidator(id);
        validator.validate();
    }

    @Override
    public void delete(int id) throws SQLException, IllegalArgumentException {
        validateId(id);
        mapper.delete(id);
    }

    @Override
    public Meal getById(int id) throws SQLException, IllegalArgumentException {
        validateId(id);
        return mapper.getByID(id);
    }

    @Override
    public List<Meal> getByUser(User user) throws SQLException {
        return mapper.getByUser(user);
    }

    @Override
    public List<Meal> getByPeriod(User user, Date startDate, Date endDate) throws IllegalArgumentException, SQLException {
        PeriodValidator validator = new PeriodValidator(startDate, endDate);
        validator.validate();

        return mapper.getByPeriod(user, startDate, endDate);
    }

    private Map<Food, Float> getTotalFoodMap(List<Meal> mealList) {
        Map<Food, Float> res = new HashMap<>();
        for (Meal meal : mealList) {
            Map<Food, Float> foodMap = meal.getFoodMap();
            util.mergeFoodMap(res, foodMap);
        }

        return res;
    }

    @Override
    public Map<Nutrient, Float> getSortedDailyNutrient(User user, Date startDate, Date endDate) throws SQLException {
        PeriodValidator validator = new PeriodValidator(startDate, endDate);
        validator.validate();

        // as requested in use case 5, we have to provide a sorted data structure for the pie diagram
        List<Meal> mealList = mapper.getByPeriod(user, startDate, endDate);
        Map<Food, Float> totalFoodMap = getTotalFoodMap(mealList);
        for (Map.Entry<Food, Float> entry : totalFoodMap.entrySet()) {
            int foodID = entry.getKey().getId();
            Map<Nutrient, Float> foodNutrientMap = service.getUnitNutrientValue(foodID);
            entry.getKey().setNutrientFloatMap(foodNutrientMap);
        }

        int days = Period.between(startDate.toLocalDate(), endDate.toLocalDate()).getDays();

        return util.getSortedDailyNutrient(totalFoodMap, days); // the result should be sorted nutrition with g as unit
    }

    @Override
    public Map<String, Float> getMealGroups(User user, Date startDate, Date endDate) throws SQLException {
        PeriodValidator validator = new PeriodValidator(startDate, endDate);
        validator.validate();
        // as requested in use case 7, we have to provide a data structure with CFG alignment, radar chart is recommended
        List<Meal> mealList = mapper.getByPeriod(user, startDate, endDate);
        Map<Food, Float> totalFoodMap = getTotalFoodMap(mealList);

        Map<String, Float> res = new HashMap<>();
        res.put("Whole Grain", 0f);
        res.put("Protein", 0f);
        res.put("Vegetables and Fruits", 0f);
        for (Map.Entry<Food, Float> entry : totalFoodMap.entrySet()) {
            String cnfGroup = entry.getKey().getGroup();
            float value = entry.getValue();
            String cfgGroup = converter.toCFGFoodGroup(cnfGroup);
            System.out.println(cfgGroup);
            res.put(cfgGroup, value + res.getOrDefault(cfgGroup, 0f));
        }

        return util.toPercentageMap(res);
    }

    @Override
    public Map<Date, Float> getCaloriesByDate(User user, Date startDate, Date endDate) throws SQLException {
        PeriodValidator validator = new PeriodValidator(startDate, endDate);
        validator.validate();
        return mapper.getCaloriesByDate(user, startDate, endDate);
    }
}
