-- Create the Supabase Storage bucket for file attachments.
-- Public bucket (MVP) — authenticated users can upload/delete, anyone can read.

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'attachments',
    'attachments',
    true,
    5242880,   -- 5 MB
    ARRAY[
        'text/plain',
        'text/markdown',
        'text/csv',
        'image/png',
        'image/jpeg',
        'image/gif',
        'image/webp'
    ]
)
ON CONFLICT (id) DO NOTHING;

-- Authenticated users can upload to this bucket
CREATE POLICY "authenticated users can upload attachments"
    ON storage.objects FOR INSERT
    TO authenticated
    WITH CHECK (bucket_id = 'attachments');

-- Public read access (URLs returned by getPublicUrl work without auth)
CREATE POLICY "public can read attachments"
    ON storage.objects FOR SELECT
    TO public
    USING (bucket_id = 'attachments');

-- Authenticated users can delete their own uploads
CREATE POLICY "authenticated users can delete own attachments"
    ON storage.objects FOR DELETE
    TO authenticated
    USING (bucket_id = 'attachments' AND auth.uid()::text = owner);
