/** Keeps stale AI menu records compatible until their icon migration has run. */
export function resolveMenuIcon(path?: string, icon?: string) {
  const normalizedPath = path ? (path.startsWith('/') ? path : `/${path}`) : ''
  if (normalizedPath === '/lowcode/ai-services' && icon === 'sparkles') return 'workflow'
  return icon || 'file'
}
