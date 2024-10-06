
public class Cipher {

    // We are implementing Cipher 11: Affine Cipher.

    public static String encrypt(String str) {

        /*
         * The user should be able to just enter the message, or type
         * "Cipher number, key, message" which in this case would be
         * "11,message" as the Affine Cipher has no key. Therefore if the
         * user input starts with "11," we encrypt only what comes after that.
         */

        if (str.substring(0, 3).equals("11,"))
            str = str.substring(3, str.length());

        // We turn string into an array of its characters.

        char[] charArray = str.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            /*
             * We turn each character into an integer of its ascii code.
             * For the encrytpion we need the character's numeric equivalent
             * from 0 to 25. To get this we substract 65 from its Ascii code
             * if it's a capital letter or we substract 97 if it's lower case.
             * We then use the encrytpion formula and return the new character
             * to the array.
             */

            int number = (int) charArray[i];
            if ((65 <= number && number <= 90)) {
                number = number - 65;
                number = ((5 * number) + 8) % 26;
                number = number + 65;

            } else if ((97 <= number && number <= 122)) {
                number = number - 97;
                number = ((5 * number) + 8) % 26;
                number = number + 97;
            }
            // We turn the ascii code back into the character and
            // return it to the array.
            char character = (char) number;
            charArray[i] = character;
        }
        // We turn the array to the new string and return it.
        // Notice we did not check for characters that are not letters or spaces.
        // As we are not required to encrypt such characters or return
        // a message that they cannot be encrypted, we simply keep them as they are.
        str = new String(charArray);
        return str;
    }

    public static String decrypt(String str) {
        char[] charArray = str.toCharArray();

        // We only decrypt the message if it contains only letters and spaces.
        // We check the string for other types of characters based on their
        // ascii codes. If such characters exist, we return a message instead.
        for (int i = 0; i < charArray.length; i++) {
            int number = (int) charArray[i];
            if ((number < 65 && number != 32) || (90 < number && number < 97) || (number > 122)) {
                System.out.println("Message cannot be decrypted");
                return str;
            }
        }
        // We follow the same process as we did in the encryption method but
        // with the decryption formula instead.

        for (int i = 0; i < charArray.length; i++) {
            int number = (int) charArray[i];
            if ((65 <= number && number <= 90)) {
                number = number - 65;
                number = (21 * (number - 8)) % 26;
                /*
                 * A modular operation with a negative can produce one of two results,
                 * with a difference of 26.
                 * For instance, -126 mod 26 can produce -22 or 4. We need 4 for
                 * the formula, but Java always returns -22. We solve this by simply
                 * doing the modular operation and adding 26.
                 */
                if (number < 0)
                    number = number + 26;
                number = number + 65;
            } else if ((97 <= number && number <= 122)) {
                number = number - 97;
                number = (21 * (number - 8)) % 26;
                if (number < 0)
                    number = number + 26;
                number = number + 97;
            }
            char character = (char) number;
            charArray[i] = character;
        }
        str = new String(charArray);
        return str;
    }

    public static void main(String[] args) {
        String str = "11, good day";

        str = encrypt(str);
        System.out.println(str);

        str = decrypt(str);
        System.out.println(str);
    }
}