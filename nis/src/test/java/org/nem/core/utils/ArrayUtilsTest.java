package org.nem.core.utils;

import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.security.InvalidParameterException;

public class ArrayUtilsTest {

    //region concat

    @Test
    public void concatCanCombineEmptyArrayWithEmptyArray() {
        // Arrange:
        byte[] lhs = new byte[] { };
        byte[] rhs = new byte[] { };

        // Act:
        byte[] result = ArrayUtils.concat(lhs, rhs);

        // Assert:
        Assert.assertThat(result, IsEqual.equalTo(new byte[] { }));
    }

    @Test
    public void concatCanCombineEmptyArrayWithNonEmptyArray() {
        // Arrange:
        byte[] lhs = new byte[] { };
        byte[] rhs = new byte[] { 12, 4, 6 };

        // Act:
        byte[] result = ArrayUtils.concat(lhs, rhs);

        // Assert:
        Assert.assertThat(result, IsEqual.equalTo(new byte[] { 12, 4, 6 }));
    }

    @Test
    public void concatCanCombineNonEmptyArrayWithEmptyArray() {
        // Arrange:
        byte[] lhs = new byte[] { 7, 13 };
        byte[] rhs = new byte[] { };

        // Act:
        byte[] result = ArrayUtils.concat(lhs, rhs);

        // Assert:
        Assert.assertThat(result, IsEqual.equalTo(new byte[] { 7, 13 }));
    }

    @Test
    public void concatCanCombineNonEmptyArrayWithNonEmptyArray() {
        // Arrange:
        byte[] lhs = new byte[] { 7, 13 };
        byte[] rhs = new byte[] { 12, 4, 6 };

        // Act:
        byte[] result = ArrayUtils.concat(lhs, rhs);

        // Assert:
        Assert.assertThat(result, IsEqual.equalTo(new byte[] { 7, 13, 12, 4, 6 }));
    }

    //endregion

    //region split

    @Test(expected = InvalidParameterException.class)
    public void splitFailsIfSplitIndexIsNegative() {
        // Arrange:
        byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

        // Act:
        ArrayUtils.split(bytes, -1);
    }

    @Test(expected = InvalidParameterException.class)
     public void splitFailsIfSplitIndexIsGreaterThanInputLength() {
        // Arrange:
        byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

        // Act:
        ArrayUtils.split(bytes, bytes.length + 1);
    }

    @Test
    public void canSplitEmptyArray() {
        // Arrange:
        byte[] bytes = new byte[] { };

        // Act:
        byte[][] parts = ArrayUtils.split(bytes, 0);

        // Assert:
        Assert.assertThat(parts.length, IsEqual.equalTo(2));
        Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { }));
        Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { }));
    }

    @Test
    public void canSplitArrayAtBeginning() {
        // Arrange:
        byte[] bytes = new byte[] { 12, 4, 6 };

        // Act:
        byte[][] parts = ArrayUtils.split(bytes, 0);

        // Assert:
        Assert.assertThat(parts.length, IsEqual.equalTo(2));
        Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { }));
        Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { 12, 4, 6 }));
    }

    @Test
    public void canSplitArrayAtEnd() {
        // Arrange:
        byte[] bytes = new byte[] { 7, 13 };

        // Act:
        byte[][] parts = ArrayUtils.split(bytes, 2);

        // Assert:
        Assert.assertThat(parts.length, IsEqual.equalTo(2));
        Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { 7, 13 }));
        Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { }));
    }

    @Test
    public void canSplitArrayAtMiddle() {
        // Arrange:
        byte[] bytes = new byte[] { 7, 13, 12, 4, 6 };

        // Act:
        byte[][] parts = ArrayUtils.split(bytes, 2);

        // Assert:
        Assert.assertThat(parts.length, IsEqual.equalTo(2));
        Assert.assertThat(parts[0], IsEqual.equalTo(new byte[] { 7, 13 }));
        Assert.assertThat(parts[1], IsEqual.equalTo(new byte[] { 12, 4, 6 }));
    }

    //endregion
}
