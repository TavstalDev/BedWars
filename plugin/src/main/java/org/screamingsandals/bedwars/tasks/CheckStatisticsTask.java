package org.screamingsandals.bedwars.tasks;

import org.screamingsandals.bedwars.Main;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CheckStatisticsTask implements Runnable {
    private LocalDateTime nextDailyReset = null;
    private LocalDateTime nextSeasonReset = null;

    public CheckStatisticsTask() {
        refreshResetTimes();
    }

    @Override
    public void run()
    {
        boolean saveConfig = false;
        if (nextDailyReset != null && LocalDateTime.now().isAfter(nextDailyReset)) {
            Main.getPlayerStatisticsManager().resetDailyScores();
            LocalDateTime nextDailyReset = LocalDate.now().plusDays(1).atStartOfDay();
            Main.getConfigurator().config.set("dates.daily-reset", nextDailyReset.toString());
            saveConfig = true;
        }

        if (nextSeasonReset != null && LocalDateTime.now().isAfter(nextSeasonReset)) {
            Main.getPlayerStatisticsManager().resetSeasonalScores();
            LocalDateTime nextSeasonReset = LocalDate.now().withDayOfMonth(1).plusMonths(1).atStartOfDay();
            Main.getConfigurator().config.set("dates.season-reset", nextSeasonReset.toString());
            saveConfig = true;
        }

        if (saveConfig)
            Main.getConfigurator().saveConfig();
    }

    private void refreshResetTimes() {
        String seasonRaw = Main.getConfigurator().config.getString("dates.season-reset");
        if (seasonRaw != null)
            nextSeasonReset = LocalDateTime.parse(seasonRaw);

        String dailyRaw = Main.getConfigurator().config.getString("dates.daily-reset");
        if (dailyRaw != null)
            nextDailyReset = LocalDateTime.parse(dailyRaw);
    }
}
