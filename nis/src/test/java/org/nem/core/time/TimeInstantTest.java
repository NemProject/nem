package org.nem.core.time;

import org.hamcrest.core.*;
import org.junit.*;

import java.security.InvalidParameterException;

public class TimeInstantTest {

    //region constants

    @Test
    public void constantsAreInitializedCorrectly() {
        // Assert:
        Assert.assertThat(TimeInstant.ZERO, IsEqual.equalTo(new TimeInstant(0)));
    }

    //endregion

    //region constructor

    @Test(expected = InvalidParameterException.class)
    public void cannotBeCreatedAroundNegativeTime() {
        // Act:
        new TimeInstant(-1);
    }

    @Test
    public void canBeCreatedAroundZeroTime() {
        // Act:
        final TimeInstant instant = new TimeInstant(0);

        // Assert:
        Assert.assertThat(instant.getRawTime(), IsEqual.equalTo(0));
    }

    @Test
    public void canBeCreatedAroundPositiveTime() {
        // Act:
        final TimeInstant instant = new TimeInstant(1);

        // Assert:
        Assert.assertThat(instant.getRawTime(), IsEqual.equalTo(1));
    }

    //endregion

    //region addX

    @Test
    public void addSecondsCreatesNewInstant() {
        // Arrange:
        final TimeInstant instant1 = new TimeInstant(7);

        // Act:
        final TimeInstant instant2 = instant1.addSeconds(2);

        // Assert:
        Assert.assertThat(instant2.getRawTime(), IsEqual.equalTo(9));
    }

    @Test
    public void addMinutesCreatesNewInstant() {
        // Arrange:
        final TimeInstant instant1 = new TimeInstant(7);

        // Act:
        final TimeInstant instant2 = instant1.addMinutes(3);

        // Assert:
        Assert.assertThat(instant2.getRawTime(), IsEqual.equalTo(187));
    }

    @Test
   public void addHoursCreatesNewInstant() {
        // Arrange:
        final TimeInstant instant1 = new TimeInstant(7);

        // Act:
        final TimeInstant instant2 = instant1.addHours(4);

        // Assert:
        Assert.assertThat(instant2.getRawTime(), IsEqual.equalTo(4*60*60 + 7));
    }

    @Test
    public void addDaysCreatesNewInstant() {
        // Arrange:
        final TimeInstant instant1 = new TimeInstant(7);

        // Act:
        final TimeInstant instant2 = instant1.addDays(5);

        // Assert:
        Assert.assertThat(instant2.getRawTime(), IsEqual.equalTo(5*24*60*60 + 7));
    }

    //endregion

    //region subtract

    @Test
    public void subtractCanSubtractEqualInstances() {
        // Arrange:
        final TimeInstant instant1 = new TimeInstant(7);
        final TimeInstant instant2 = new TimeInstant(7);

        // Assert:
        Assert.assertThat(instant1.subtract(instant2), IsEqual.equalTo(0));
        Assert.assertThat(instant2.subtract(instant1), IsEqual.equalTo(0));
    }

    @Test
    public void subtractCanSubtractUnequalInstances() {
        // Arrange:
        final TimeInstant instant1 = new TimeInstant(7);
        final TimeInstant instant2 = new TimeInstant(11);

        // Assert:
        Assert.assertThat(instant1.subtract(instant2), IsEqual.equalTo(-4));
        Assert.assertThat(instant2.subtract(instant1), IsEqual.equalTo(4));
    }

    //endregion

    //region compareTo

    @Test
    public void compareToCanCompareEqualInstances() {
        // Arrange:
        final TimeInstant instant1 = new TimeInstant(7);
        final TimeInstant instant2 = new TimeInstant(7);

        // Assert:
        Assert.assertThat(instant1.compareTo(instant2), IsEqual.equalTo(0));
        Assert.assertThat(instant2.compareTo(instant1), IsEqual.equalTo(0));
    }

    @Test
    public void compareToCanCompareUnequalInstances() {
        // Arrange:
        final TimeInstant instant1 = new TimeInstant(7);
        final TimeInstant instant2 = new TimeInstant(8);

        // Assert:
        Assert.assertThat(instant1.compareTo(instant2), IsEqual.equalTo(-1));
        Assert.assertThat(instant2.compareTo(instant1), IsEqual.equalTo(1));
    }

    //endregion

    //region equals / hashCode

    @Test
    public void equalsOnlyReturnsTrueForEquivalentObjects() {
        // Arrange:
        final TimeInstant instant = new TimeInstant(7);

        // Assert:
        Assert.assertThat(new TimeInstant(7), IsEqual.equalTo(instant));
        Assert.assertThat(new TimeInstant(6), IsNot.not(IsEqual.equalTo(instant)));
        Assert.assertThat(new TimeInstant(8), IsNot.not(IsEqual.equalTo(instant)));
        Assert.assertThat(null, IsNot.not(IsEqual.equalTo(instant)));
        Assert.assertThat(7, IsNot.not(IsEqual.equalTo((Object)instant)));
    }

    @Test
    public void hashCodesAreOnlyEqualForEquivalentObjects() {
        // Arrange:
        final TimeInstant instant = new TimeInstant(7);
        final int hashCode = instant.hashCode();

        // Assert:
        Assert.assertThat(new TimeInstant(7).hashCode(), IsEqual.equalTo(hashCode));
        Assert.assertThat(new TimeInstant(6).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
        Assert.assertThat(new TimeInstant(8).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
    }

    //endregion

    //region toString

    @Test
    public void toStringReturnsRawTime() {
        // Arrange:
        final TimeInstant instant = new TimeInstant(22561);

        // Assert:
        Assert.assertThat(instant.toString(), IsEqual.equalTo("22561"));
    }

    //endregion
}