package net.sqs2.translator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * He;per class for holding results of the bundle names calculation
 * Immutable, used by only one thread
 */
final class BundleNames
{
    private final Locale requestedFor; // This is not read by anyone at the moment..
    final  List<Locale> locales;
    final List<String> names;

    BundleNames(final Locale requestedFor)
    {
        this.requestedFor=requestedFor;
        locales = new ArrayList<Locale>();
        names = new ArrayList<String>();
    }
    void addBundleName(final String language,
        final String country,
        final String variant,
        final StringBuffer nameBuffer)
    {
        final String name = nameBuffer.toString();
        final Locale locale = (variant==null) ? new Locale(language,country) : new Locale(language,country,variant);
        locales.add(0, locale);
        names.add(0,name);
    }
    /**
     * Calculate the bundle names
     * @param baseName  the base bundle name
     * @param locale    the locale
     * @param names     the list used to return the names of the bundles
     *
     */
    static BundleNames calculateBundleNames(final String baseName, final Locale locale) {

        final BundleNames result = new BundleNames(locale);
        final String language = locale.getLanguage();
        final int languageLength = language.length();
        final String country = locale.getCountry();
        final int countryLength = country.length();
        final String variant = locale.getVariant();
        final int variantLength = variant.length();

        if (languageLength + countryLength + variantLength == 0) {
            //The locale is "", "", "".
            return result;
        }
        final StringBuffer temp = new StringBuffer(baseName);
        temp.append('_');
        temp.append(language);
        result.addBundleName(language, "", null, temp);

        if (countryLength + variantLength == 0) {
            return result;
        }
        temp.append('_');
        temp.append(country);
        result.addBundleName(language, country, null, temp);

        if (variantLength == 0) {
            return result;
        }
        temp.append('_');
        temp.append(variant);
        result.addBundleName(language, country, variant, temp);

        return result;
    }
}