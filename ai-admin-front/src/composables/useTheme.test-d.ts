import { type BrandTheme, type Theme, useTheme } from './useTheme'

function acceptsTheme(value: Theme) {
  return value
}

function acceptsBrand(value: BrandTheme) {
  return value
}

const themeApi = useTheme()

acceptsTheme(themeApi.theme.value)
acceptsBrand(themeApi.brand.value)
acceptsBrand('tech-purple')
acceptsBrand('metro-green')
acceptsBrand('aurora-cyan')
acceptsBrand('nebula-violet')
acceptsBrand('coral-rose')
acceptsBrand('solar-gold')
acceptsBrand('deep-ocean')

themeApi.setBrand('tech-purple')
themeApi.setBrand('metro-green')
themeApi.setBrand('aurora-cyan')
themeApi.setBrand('nebula-violet')
themeApi.setBrand('coral-rose')
themeApi.setBrand('solar-gold')
themeApi.setBrand('deep-ocean')
themeApi.toggleTheme()

themeApi.brandOptions.forEach((option) => {
  acceptsBrand(option.value)
  option.label.toUpperCase()
})

// @ts-expect-error unsupported brand names should not compile
themeApi.setBrand('blue')

// @ts-expect-error unsupported display modes should not compile
acceptsTheme('system')
