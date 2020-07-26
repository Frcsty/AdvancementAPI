package com.github.frcsty.advancementapi.wrapper.component;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import net.minecraft.server.v1_16_R1.Criterion;
import net.minecraft.server.v1_16_R1.CriterionInstance;
import net.minecraft.server.v1_16_R1.LootSerializationContext;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import org.bukkit.Warning;

import java.util.*;

public final class CriteriaComponent {

    @SerializedName("criteriaAmount")
    private int criteria = 1;
    @SerializedName("criteria")
    private Set<String> savedCriterionNames = null;
    @SerializedName("criteriaRequirements")
    private String[][] savedCriteriaRequirements = null;
    private transient Map<String, Criterion> savedCriteria = null;
    private transient Map<String, Set<String>> awardedCriteria = new HashMap<>();

    @Warning(reason = "Only use if you know what you are doing!")
    public void saveCriteria(final Map<String, Criterion> save) {
        savedCriteria = save;
        savedCriterionNames = save.keySet();
    }

    public Map<String, Criterion> getSavedCriteria() {
        return savedCriteria;
    }

    @Warning(reason = "Only use if you know what you are doing!")
    public void saveCriteriaRequirements(final String[][] save) {
        savedCriteriaRequirements = save;
    }

    public String[][] getSavedCriteriaRequirements() {
        return savedCriteriaRequirements;
    }

    public Map<String, Set<String>> getAwardedCriteria() {
        return awardedCriteria == null ? new HashMap<>() : awardedCriteria;
    }

    public void setAwardedCriteria(final Map<String, Set<String>> awardedCriteria) {
        this.awardedCriteria = awardedCriteria;
    }

    public int getCriteria() {
        return criteria;
    }

    public void setCriteria(final int criteria) {
        this.criteria = criteria;
        final Map<String, Criterion> advCriteria = new HashMap<>();
        final String[][] advRequirements;

        for (int i = 0; i < getCriteria(); i++) {
            advCriteria.put("criterion." + i, new Criterion(new CriterionInstance() {
                @Override
                public JsonObject a(final LootSerializationContext context) {
                    return null;
                }

                @Override
                public MinecraftKey a() {
                    return new MinecraftKey("minecraft", "impossible");
                }
            }));
        }
        saveCriteria(advCriteria);

        final List<String[]> fixedRequirements = new ArrayList<>();
        for (final String name : advCriteria.keySet()) {
            fixedRequirements.add(new String[]{name});
        }
        advRequirements = Arrays.stream(fixedRequirements.toArray()).toArray(String[][]::new);
        saveCriteriaRequirements(advRequirements);
    }
}
